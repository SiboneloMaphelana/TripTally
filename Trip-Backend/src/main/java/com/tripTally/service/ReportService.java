package com.tripTally.service;

import com.tripTally.domain.entity.Expense;
import com.tripTally.domain.entity.ExpenseCategory;
import com.tripTally.domain.entity.Trip;
import com.tripTally.domain.entity.TripMember;
import com.tripTally.domain.entity.User;
import com.tripTally.dto.expense.ExpenseResponse;
import com.tripTally.dto.summary.TripSummaryResponse;
import com.tripTally.mapper.DtoMapper;
import com.tripTally.repository.ExpenseRepository;
import com.tripTally.repository.ReceiptAttachmentRepository;
import com.tripTally.repository.TripMemberRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

	private static final RoundingMode RM = RoundingMode.HALF_UP;

	private final TripAccessService tripAccessService;
	private final ExpenseRepository expenseRepository;
	private final TripMemberRepository tripMemberRepository;
	private final ReceiptAttachmentRepository receiptAttachmentRepository;
	private final DtoMapper dtoMapper;

	public ReportService(
			TripAccessService tripAccessService,
			ExpenseRepository expenseRepository,
			TripMemberRepository tripMemberRepository,
			ReceiptAttachmentRepository receiptAttachmentRepository,
			DtoMapper dtoMapper) {
		this.tripAccessService = tripAccessService;
		this.expenseRepository = expenseRepository;
		this.tripMemberRepository = tripMemberRepository;
		this.receiptAttachmentRepository = receiptAttachmentRepository;
		this.dtoMapper = dtoMapper;
	}

	@Transactional(readOnly = true)
	public TripSummaryResponse summary(Long tripId, User user) {
		Trip trip = tripAccessService.requireTripMember(tripId, user);
		List<Expense> expenses = expenseRepository.findAllForLedger(trip);
		Map<Long, String> labels = tripMemberRepository.findByTripOrderByCreatedAtAsc(trip).stream()
				.collect(Collectors.toMap(TripMember::getId, TripMember::getDisplayName));

		BigDecimal total = BigDecimal.ZERO.setScale(2, RM);
		Map<ExpenseCategory, BigDecimal> byCategory = new EnumMap<>(ExpenseCategory.class);
		Map<Long, BigDecimal> byMember = new HashMap<>();

		for (Expense e : expenses) {
			total = total.add(e.getAmount());
			byCategory.merge(e.getCategory(), e.getAmount(), BigDecimal::add);
			byMember.merge(e.getPayer().getId(), e.getAmount(), BigDecimal::add);
		}
		for (ExpenseCategory c : ExpenseCategory.values()) {
			byCategory.putIfAbsent(c, BigDecimal.ZERO.setScale(2, RM));
		}
		byCategory.replaceAll((k, v) -> v.setScale(2, RM));
		byMember.replaceAll((k, v) -> v.setScale(2, RM));
		total = total.setScale(2, RM);

		List<Expense> latest = expenses.stream()
				.sorted(Comparator.comparing(Expense::getExpenseDate).reversed()
						.thenComparing(Expense::getId, Comparator.reverseOrder()))
				.limit(8)
				.toList();
		List<Long> latestIds = latest.stream().map(Expense::getId).toList();
		Set<Long> receiptIds = latestIds.isEmpty()
				? Set.of()
				: new HashSet<>(receiptAttachmentRepository.findExpenseIdsHavingReceipt(latestIds));
		List<ExpenseResponse> latestDtos = latest.stream()
				.map(e -> dtoMapper.toExpense(e, labels, receiptIds.contains(e.getId())))
				.toList();

		return TripSummaryResponse.builder()
				.currencyCode(trip.getCurrencyCode())
				.totalSpend(total)
				.spendByCategory(byCategory)
				.spendByMember(byMember)
				.latestExpenses(latestDtos)
				.build();
	}

	@Transactional(readOnly = true)
	public byte[] exportCsv(Long tripId, User user) {
		Trip trip = tripAccessService.requireTripMember(tripId, user);
		List<Expense> expenses = expenseRepository.findAllForLedger(trip);
		expenses.sort(Comparator.comparing(Expense::getExpenseDate).thenComparing(Expense::getId));
		Map<Long, String> labels = tripMemberRepository.findByTripOrderByCreatedAtAsc(trip).stream()
				.collect(Collectors.toMap(TripMember::getId, TripMember::getDisplayName));

		StringBuilder sb = new StringBuilder();
		sb.append("date,description,category,amount,currency,payer,settled,split_mode,participants\n");
		for (Expense e : expenses) {
			String participants = e.getParticipants().stream()
					.map(p -> labels.getOrDefault(p.getTripMember().getId(), "?") + ":" + p.getOwedAmount())
					.collect(Collectors.joining(";"));
			sb.append(csv(e.getExpenseDate()))
					.append(',')
					.append(csv(e.getDescription()))
					.append(',')
					.append(csv(e.getCategory().name()))
					.append(',')
					.append(e.getAmount().toPlainString())
					.append(',')
					.append(csv(trip.getCurrencyCode()))
					.append(',')
					.append(csv(labels.getOrDefault(e.getPayer().getId(), "?")))
					.append(',')
					.append(e.isSettled())
					.append(',')
					.append(csv(e.getSplitMode().name()))
					.append(',')
					.append(csv(participants))
					.append('\n');
		}
		return sb.toString().getBytes(StandardCharsets.UTF_8);
	}

	private static String csv(Object value) {
		String s = String.valueOf(value);
		if (s.contains("\"") || s.contains(",") || s.contains("\n") || s.contains("\r")) {
			return "\"" + s.replace("\"", "\"\"") + "\"";
		}
		return s;
	}
}
