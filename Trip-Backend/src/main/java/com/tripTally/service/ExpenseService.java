package com.tripTally.service;

import com.tripTally.domain.entity.Expense;
import com.tripTally.domain.entity.ExpenseCategory;
import com.tripTally.domain.entity.ExpenseParticipant;
import com.tripTally.domain.entity.Trip;
import com.tripTally.domain.entity.TripMember;
import com.tripTally.domain.entity.User;
import com.tripTally.dto.common.PagedResponse;
import com.tripTally.dto.expense.ExpenseCreateRequest;
import com.tripTally.dto.expense.ExpenseParticipantRequest;
import com.tripTally.dto.expense.ExpenseResponse;
import com.tripTally.dto.expense.ExpenseUpdateRequest;
import com.tripTally.exception.ApiException;
import com.tripTally.mapper.DtoMapper;
import com.tripTally.repository.ExpenseParticipantRepository;
import com.tripTally.repository.ExpenseRepository;
import com.tripTally.repository.ExpenseSpecifications;
import com.tripTally.repository.ReceiptAttachmentRepository;
import com.tripTally.repository.TripMemberRepository;
import com.tripTally.service.split.CalculatedOwed;
import com.tripTally.service.split.ExpenseSplitCalculator;
import com.tripTally.service.split.SplitParticipantInput;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

	private final com.tripTally.service.TripAccessService tripAccessService;
	private final ExpenseRepository expenseRepository;
	private final ExpenseParticipantRepository expenseParticipantRepository;
	private final TripMemberRepository tripMemberRepository;
	private final ExpenseSplitCalculator splitCalculator;
	private final DtoMapper dtoMapper;
	private final ReceiptAttachmentRepository receiptAttachmentRepository;

	public ExpenseService(
			TripAccessService tripAccessService,
			ExpenseRepository expenseRepository,
			ExpenseParticipantRepository expenseParticipantRepository,
			TripMemberRepository tripMemberRepository,
			ExpenseSplitCalculator splitCalculator,
			DtoMapper dtoMapper,
			ReceiptAttachmentRepository receiptAttachmentRepository) {
		this.tripAccessService = tripAccessService;
		this.expenseRepository = expenseRepository;
		this.expenseParticipantRepository = expenseParticipantRepository;
		this.tripMemberRepository = tripMemberRepository;
		this.splitCalculator = splitCalculator;
		this.dtoMapper = dtoMapper;
		this.receiptAttachmentRepository = receiptAttachmentRepository;
	}

	@Transactional(readOnly = true)
	public PagedResponse<ExpenseResponse> list(
			Long tripId,
			User user,
			int page,
			int size,
			ExpenseCategory category,
			Long payerMemberId,
			Long participantMemberId,
			LocalDate dateFrom,
			LocalDate dateTo,
			Boolean settled) {
		Trip trip = tripAccessService.requireTripMember(tripId, user);
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "expenseDate", "id"));
		var spec = ExpenseSpecifications.build(trip, category, payerMemberId, participantMemberId, dateFrom, dateTo, settled);
		Page<Expense> result = expenseRepository.findAll(spec, pageable);
		Map<Long, String> labels = memberLabels(trip);
		List<Long> ids = result.getContent().stream().map(Expense::getId).toList();
		Set<Long> receiptIds = ids.isEmpty()
				? Set.of()
				: new HashSet<>(receiptAttachmentRepository.findExpenseIdsHavingReceipt(ids));
		List<ExpenseResponse> content = result.getContent().stream()
				.map(e -> dtoMapper.toExpense(e, labels, receiptIds.contains(e.getId())))
				.toList();
		return PagedResponse.<ExpenseResponse>builder()
				.content(content)
				.page(result.getNumber())
				.size(result.getSize())
				.totalElements(result.getTotalElements())
				.totalPages(result.getTotalPages())
				.build();
	}

	@Transactional(readOnly = true)
	public ExpenseResponse get(Long expenseId, User user) {
		Expense expense = expenseRepository.findById(expenseId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Expense not found"));
		tripAccessService.requireTripMember(expense.getTrip().getId(), user);
		Map<Long, String> labels = memberLabels(expense.getTrip());
		boolean hasReceipt = !receiptAttachmentRepository.findExpenseIdsHavingReceipt(List.of(expenseId)).isEmpty();
		return dtoMapper.toExpense(expense, labels, hasReceipt);
	}

	@Transactional
	public ExpenseResponse create(Long tripId, User user, ExpenseCreateRequest request) {
		Trip trip = tripAccessService.requireTripMember(tripId, user);
		TripMember payer = loadMember(trip, request.getPayerMemberId());
		List<SplitParticipantInput> inputs = toSplitInputs(request.getParticipants());
		validateParticipantMembers(trip, inputs.stream().map(SplitParticipantInput::getTripMemberId).toList());
		List<CalculatedOwed> calculated = splitCalculator.calculate(request.getSplitMode(), request.getAmount(), inputs);

		Expense expense = Expense.builder()
				.trip(trip)
				.payer(payer)
				.amount(request.getAmount().setScale(2, java.math.RoundingMode.HALF_UP))
				.category(request.getCategory())
				.description(request.getDescription().trim())
				.expenseDate(request.getExpenseDate())
				.splitMode(request.getSplitMode())
				.settled(request.isSettled())
				.build();
		expense = expenseRepository.save(expense);
		saveParticipants(expense, calculated);
		expense = expenseRepository.findById(expense.getId()).orElseThrow();
		Map<Long, String> labels = memberLabels(trip);
		boolean hasReceipt = false;
		return dtoMapper.toExpense(expense, labels, hasReceipt);
	}

	@Transactional
	public ExpenseResponse update(Long expenseId, User user, ExpenseUpdateRequest request) {
		Expense expense = expenseRepository.findById(expenseId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Expense not found"));
		Trip trip = expense.getTrip();
		tripAccessService.requireTripMember(trip.getId(), user);
		TripMember payer = loadMember(trip, request.getPayerMemberId());
		List<SplitParticipantInput> inputs = toSplitInputs(request.getParticipants());
		validateParticipantMembers(trip, inputs.stream().map(SplitParticipantInput::getTripMemberId).toList());
		List<CalculatedOwed> calculated = splitCalculator.calculate(request.getSplitMode(), request.getAmount(), inputs);

		expense.setPayer(payer);
		expense.setAmount(request.getAmount().setScale(2, java.math.RoundingMode.HALF_UP));
		expense.setCategory(request.getCategory());
		expense.setDescription(request.getDescription().trim());
		expense.setExpenseDate(request.getExpenseDate());
		expense.setSplitMode(request.getSplitMode());
		expense.setSettled(request.isSettled());
		expenseRepository.save(expense);
		expenseParticipantRepository.deleteByExpense(expense);
		saveParticipants(expense, calculated);
		expense = expenseRepository.findById(expense.getId()).orElseThrow();
		Map<Long, String> labels = memberLabels(trip);
		boolean hasReceipt = !receiptAttachmentRepository.findExpenseIdsHavingReceipt(List.of(expenseId)).isEmpty();
		return dtoMapper.toExpense(expense, labels, hasReceipt);
	}

	@Transactional
	public void delete(Long expenseId, User user) {
		Expense expense = expenseRepository.findById(expenseId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Expense not found"));
		tripAccessService.requireTripMember(expense.getTrip().getId(), user);
		receiptAttachmentRepository.findByExpense(expense).ifPresent(receiptAttachmentRepository::delete);
		expenseRepository.delete(expense);
	}

	private void saveParticipants(Expense expense, List<CalculatedOwed> calculated) {
		for (CalculatedOwed c : calculated) {
			TripMember m = tripMemberRepository.findByIdAndTrip(c.getTripMemberId(), expense.getTrip())
					.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid trip member"));
			ExpenseParticipant ep = ExpenseParticipant.builder()
					.expense(expense)
					.tripMember(m)
					.owedAmount(c.getOwedAmount())
					.splitInput(c.getSplitInputStored())
					.build();
			expenseParticipantRepository.save(ep);
		}
	}

	private TripMember loadMember(Trip trip, Long memberId) {
		return tripMemberRepository.findByIdAndTrip(memberId, trip)
				.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Payer must be a member of this trip"));
	}

	private void validateParticipantMembers(Trip trip, List<Long> memberIds) {
		List<TripMember> found = tripMemberRepository.findByTripAndIdIn(trip, memberIds);
		if (found.size() != new HashSet<>(memberIds).size()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Participants must be members of this trip");
		}
	}

	private List<SplitParticipantInput> toSplitInputs(List<ExpenseParticipantRequest> list) {
		return list.stream()
				.map(p -> SplitParticipantInput.builder()
						.tripMemberId(p.getTripMemberId())
						.splitInput(p.getSplitInput() != null ? p.getSplitInput() : BigDecimal.ZERO)
						.build())
				.toList();
	}

	private Map<Long, String> memberLabels(Trip trip) {
		return tripMemberRepository.findByTripOrderByCreatedAtAsc(trip).stream()
				.collect(Collectors.toMap(TripMember::getId, TripMember::getDisplayName));
	}
}
