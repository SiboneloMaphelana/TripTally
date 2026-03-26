package com.tripTally.service;

import com.tripTally.domain.entity.NotificationType;
import com.tripTally.domain.entity.PaymentRequest;
import com.tripTally.domain.entity.PaymentRequestStatus;
import com.tripTally.domain.entity.Trip;
import com.tripTally.domain.entity.TripMember;
import com.tripTally.domain.entity.User;
import com.tripTally.dto.payment.PaymentRequestCreateRequest;
import com.tripTally.dto.payment.PaymentRequestResponse;
import com.tripTally.exception.ApiException;
import com.tripTally.mapper.DtoMapper;
import com.tripTally.repository.PaymentRequestRepository;
import com.tripTally.repository.TripMemberRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentRequestService {

	private final TripAccessService tripAccessService;
	private final TripMemberRepository tripMemberRepository;
	private final PaymentRequestRepository paymentRequestRepository;
	private final NotificationService notificationService;
	private final DtoMapper dtoMapper;

	public PaymentRequestService(
			TripAccessService tripAccessService,
			TripMemberRepository tripMemberRepository,
			PaymentRequestRepository paymentRequestRepository,
			NotificationService notificationService,
			DtoMapper dtoMapper) {
		this.tripAccessService = tripAccessService;
		this.tripMemberRepository = tripMemberRepository;
		this.paymentRequestRepository = paymentRequestRepository;
		this.notificationService = notificationService;
		this.dtoMapper = dtoMapper;
	}

	@Transactional(readOnly = true)
	public List<PaymentRequestResponse> list(Long tripId, User user) {
		Trip trip = tripAccessService.requireTripMember(tripId, user);
		return paymentRequestRepository.findByTripOrderByCreatedAtDesc(trip).stream()
				.map(dtoMapper::toPaymentRequest)
				.toList();
	}

	@Transactional
	public PaymentRequestResponse create(Long tripId, User user, PaymentRequestCreateRequest request) {
		Trip trip = tripAccessService.requireTripMember(tripId, user);
		TripMember creditor = tripMemberRepository
				.findByTripAndUser_Id(trip, user.getId())
				.orElseThrow(() -> new ApiException(
						HttpStatus.FORBIDDEN, "Could not resolve your traveler profile on this trip."));
		TripMember debtor = tripMemberRepository
				.findByIdAndTrip(request.getDebtorMemberId(), trip)
				.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "That person is not on this trip."));
		if (creditor.getId().equals(debtor.getId())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot request payment from yourself.");
		}
		if (debtor.getUser() == null) {
			throw new ApiException(
					HttpStatus.BAD_REQUEST,
					"That traveler has not linked a TripTally account yet, so they cannot receive payment requests. Invite their email or ask them to sign up.");
		}
		BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_UP);
		if (amount.signum() <= 0) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Amount must be positive.");
		}
		String note = request.getNote() != null ? request.getNote().trim() : null;
		if (note != null && note.isEmpty()) {
			note = null;
		}

		PaymentRequest pr = PaymentRequest.builder()
				.trip(trip)
				.creditorMember(creditor)
				.debtorMember(debtor)
				.amount(amount)
				.note(note)
				.requestedBy(user)
				.status(PaymentRequestStatus.PENDING)
				.build();
		pr = paymentRequestRepository.save(pr);

		String currency = trip.getCurrencyCode();
		String tripTitle = trip.getTitle();
		String amt = amount.toPlainString();
		String noteClause = note != null ? " Note: " + note : "";

		notificationService.notify(
				debtor.getUser(),
				NotificationType.PAYMENT_REQUEST_RECEIVED,
				"Payment request",
				String.format(
						"%s asked you to send %s %s for \"%s\".%s",
						creditor.getDisplayName(),
						currency,
						amt,
						tripTitle,
						noteClause),
				trip);

		notificationService.notify(
				user,
				NotificationType.PAYMENT_REQUEST_SENT,
				"Request sent",
				String.format(
						"You asked %s to pay %s %s for \"%s\".%s",
						debtor.getDisplayName(),
						currency,
						amt,
						tripTitle,
						noteClause),
				trip);

		return dtoMapper.toPaymentRequest(pr);
	}
}
