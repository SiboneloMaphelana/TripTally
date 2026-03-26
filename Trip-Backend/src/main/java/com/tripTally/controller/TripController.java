package com.tripTally.controller;

import com.tripTally.domain.entity.ExpenseCategory;
import com.tripTally.dto.common.PagedResponse;
import com.tripTally.dto.expense.ExpenseCreateRequest;
import com.tripTally.dto.expense.ExpenseResponse;
import com.tripTally.dto.member.TripMemberCreateRequest;
import com.tripTally.dto.member.TripMemberResponse;
import com.tripTally.dto.member.TripMemberSelfResponse;
import com.tripTally.dto.payment.PaymentRequestCreateRequest;
import com.tripTally.dto.payment.PaymentRequestResponse;
import com.tripTally.dto.settlement.SettlementCreateRequest;
import com.tripTally.dto.settlement.SettlementResponse;
import com.tripTally.dto.settlement.SettlementSuggestionResponse;
import com.tripTally.dto.summary.TripSummaryResponse;
import com.tripTally.dto.trip.TripCreateRequest;
import com.tripTally.dto.trip.TripResponse;
import com.tripTally.dto.trip.TripUpdateRequest;
import com.tripTally.dto.balance.TripBalancesResponse;
import com.tripTally.service.CurrentUserService;
import com.tripTally.service.ExpenseService;
import com.tripTally.service.ReportService;
import com.tripTally.service.PaymentRequestService;
import com.tripTally.service.SettlementRecordingService;
import com.tripTally.service.TripLedgerService;
import com.tripTally.service.TripMemberService;
import com.tripTally.service.TripService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
public class TripController {

	private final TripService tripService;
	private final TripMemberService tripMemberService;
	private final ExpenseService expenseService;
	private final TripLedgerService tripLedgerService;
	private final SettlementRecordingService settlementRecordingService;
	private final PaymentRequestService paymentRequestService;
	private final ReportService reportService;
	private final CurrentUserService currentUserService;

	public TripController(
			TripService tripService,
			TripMemberService tripMemberService,
			ExpenseService expenseService,
			TripLedgerService tripLedgerService,
			SettlementRecordingService settlementRecordingService,
			PaymentRequestService paymentRequestService,
			ReportService reportService,
			CurrentUserService currentUserService) {
		this.tripService = tripService;
		this.tripMemberService = tripMemberService;
		this.expenseService = expenseService;
		this.tripLedgerService = tripLedgerService;
		this.settlementRecordingService = settlementRecordingService;
		this.paymentRequestService = paymentRequestService;
		this.reportService = reportService;
		this.currentUserService = currentUserService;
	}

	@GetMapping
	public List<TripResponse> listTrips() {
		return tripService.listForUser(currentUserService.requireUser());
	}

	@PostMapping
	public TripResponse createTrip(@Valid @RequestBody TripCreateRequest request) {
		return tripService.create(currentUserService.requireUser(), request);
	}

	@GetMapping("/{tripId}")
	public TripResponse getTrip(@PathVariable Long tripId) {
		return tripService.get(tripId, currentUserService.requireUser());
	}

	@PutMapping("/{tripId}")
	public TripResponse updateTrip(@PathVariable Long tripId, @Valid @RequestBody TripUpdateRequest request) {
		return tripService.update(tripId, currentUserService.requireUser(), request);
	}

	@DeleteMapping("/{tripId}")
	public ResponseEntity<Void> deleteTrip(@PathVariable Long tripId) {
		tripService.delete(tripId, currentUserService.requireUser());
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{tripId}/members")
	public TripMemberResponse addMember(@PathVariable Long tripId, @Valid @RequestBody TripMemberCreateRequest request) {
		return tripMemberService.add(tripId, currentUserService.requireUser(), request);
	}

	@GetMapping("/{tripId}/members")
	public List<TripMemberResponse> listMembers(@PathVariable Long tripId) {
		return tripMemberService.list(tripId, currentUserService.requireUser());
	}

	@GetMapping("/{tripId}/members/me")
	public TripMemberSelfResponse myMember(@PathVariable Long tripId) {
		return tripMemberService.selfMember(tripId, currentUserService.requireUser());
	}

	@GetMapping("/{tripId}/payment-requests")
	public List<PaymentRequestResponse> listPaymentRequests(@PathVariable Long tripId) {
		return paymentRequestService.list(tripId, currentUserService.requireUser());
	}

	@PostMapping("/{tripId}/payment-requests")
	public PaymentRequestResponse createPaymentRequest(
			@PathVariable Long tripId,
			@Valid @RequestBody PaymentRequestCreateRequest request) {
		return paymentRequestService.create(tripId, currentUserService.requireUser(), request);
	}

	@GetMapping("/{tripId}/expenses")
	public PagedResponse<ExpenseResponse> listExpenses(
			@PathVariable Long tripId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) ExpenseCategory category,
			@RequestParam(required = false) Long payerMemberId,
			@RequestParam(required = false) Long participantMemberId,
			@RequestParam(required = false) LocalDate dateFrom,
			@RequestParam(required = false) LocalDate dateTo,
			@RequestParam(required = false) Boolean settled) {
		return expenseService.list(
				tripId,
				currentUserService.requireUser(),
				page,
				size,
				category,
				payerMemberId,
				participantMemberId,
				dateFrom,
				dateTo,
				settled);
	}

	@PostMapping("/{tripId}/expenses")
	public ExpenseResponse createExpense(@PathVariable Long tripId, @Valid @RequestBody ExpenseCreateRequest request) {
		return expenseService.create(tripId, currentUserService.requireUser(), request);
	}

	@GetMapping("/{tripId}/balances")
	public TripBalancesResponse balances(@PathVariable Long tripId) {
		return tripLedgerService.balances(tripId, currentUserService.requireUser());
	}

	@GetMapping("/{tripId}/settlement-suggestions")
	public List<SettlementSuggestionResponse> suggestions(@PathVariable Long tripId) {
		return tripLedgerService.suggestions(tripId, currentUserService.requireUser());
	}

	@GetMapping("/{tripId}/settlements")
	public List<SettlementResponse> listSettlements(@PathVariable Long tripId) {
		return settlementRecordingService.list(tripId, currentUserService.requireUser());
	}

	@PostMapping("/{tripId}/settlements")
	public SettlementResponse recordSettlement(
			@PathVariable Long tripId,
			@Valid @RequestBody SettlementCreateRequest request) {
		return settlementRecordingService.record(tripId, currentUserService.requireUser(), request);
	}

	@GetMapping("/{tripId}/summary")
	public TripSummaryResponse summary(@PathVariable Long tripId) {
		return reportService.summary(tripId, currentUserService.requireUser());
	}

	@GetMapping(value = "/{tripId}/export/csv", produces = "text/csv")
	public ResponseEntity<byte[]> exportCsv(@PathVariable Long tripId) {
		byte[] data = reportService.exportCsv(tripId, currentUserService.requireUser());
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"triptally-trip-" + tripId + ".csv\"")
				.contentType(MediaType.parseMediaType("text/csv"))
				.body(data);
	}
}
