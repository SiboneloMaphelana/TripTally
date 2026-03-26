package com.tripTally.mapper;

import com.tripTally.domain.entity.Expense;
import com.tripTally.domain.entity.ExpenseParticipant;
import com.tripTally.domain.entity.InAppNotification;
import com.tripTally.domain.entity.PaymentRequest;
import com.tripTally.domain.entity.Settlement;
import com.tripTally.domain.entity.Trip;
import com.tripTally.domain.entity.TripMember;
import com.tripTally.domain.entity.User;
import com.tripTally.dto.auth.UserResponse;
import com.tripTally.dto.balance.MemberBalanceResponse;
import com.tripTally.dto.expense.ExpenseParticipantResponse;
import com.tripTally.dto.expense.ExpenseResponse;
import com.tripTally.dto.member.TripMemberResponse;
import com.tripTally.dto.notification.NotificationResponse;
import com.tripTally.dto.payment.PaymentRequestResponse;
import com.tripTally.dto.settlement.SettlementResponse;
import com.tripTally.dto.settlement.SettlementSuggestionResponse;
import com.tripTally.dto.trip.TripResponse;
import com.tripTally.service.settlement.SettlementSuggestion;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {

	public UserResponse toUser(User user) {
		return UserResponse.builder()
				.id(user.getId())
				.email(user.getEmail())
				.displayName(user.getDisplayName())
				.build();
	}

	public TripResponse toTrip(Trip trip) {
		return TripResponse.builder()
				.id(trip.getId())
				.title(trip.getTitle())
				.destination(trip.getDestination())
				.startDate(trip.getStartDate())
				.endDate(trip.getEndDate())
				.coverImagePath(trip.getCoverImagePath())
				.currencyCode(trip.getCurrencyCode())
				.ownerUserId(trip.getOwner().getId())
				.build();
	}

	public TripMemberResponse toMember(TripMember m) {
		return TripMemberResponse.builder()
				.id(m.getId())
				.displayName(m.getDisplayName())
				.invitedEmail(m.getInvitedEmail())
				.role(m.getRole())
				.linkedUserId(m.getUser() != null ? m.getUser().getId() : null)
				.build();
	}

	public ExpenseResponse toExpense(Expense e, Map<Long, String> memberLabels, boolean hasReceipt) {
		List<ExpenseParticipantResponse> parts = e.getParticipants().stream()
				.map(p -> toParticipant(p, memberLabels))
				.collect(Collectors.toList());
		return ExpenseResponse.builder()
				.id(e.getId())
				.tripId(e.getTrip().getId())
				.payerMemberId(e.getPayer().getId())
				.payerLabel(memberLabels.getOrDefault(e.getPayer().getId(), e.getPayer().getDisplayName()))
				.amount(e.getAmount())
				.category(e.getCategory())
				.description(e.getDescription())
				.expenseDate(e.getExpenseDate())
				.splitMode(e.getSplitMode())
				.settled(e.isSettled())
				.hasReceipt(hasReceipt)
				.participants(parts)
				.build();
	}

	private ExpenseParticipantResponse toParticipant(ExpenseParticipant p, Map<Long, String> labels) {
		Long mid = p.getTripMember().getId();
		return ExpenseParticipantResponse.builder()
				.tripMemberId(mid)
				.memberLabel(labels.getOrDefault(mid, p.getTripMember().getDisplayName()))
				.owedAmount(p.getOwedAmount())
				.splitInput(p.getSplitInput())
				.build();
	}

	public MemberBalanceResponse toBalance(
			TripMember m,
			java.math.BigDecimal paid,
			java.math.BigDecimal owed,
			java.math.BigDecimal net) {
		return MemberBalanceResponse.builder()
				.tripMemberId(m.getId())
				.label(m.getDisplayName())
				.totalPaid(paid)
				.totalOwed(owed)
				.netBalance(net)
				.build();
	}

	public SettlementResponse toSettlement(Settlement s) {
		return SettlementResponse.builder()
				.id(s.getId())
				.fromMemberId(s.getFromMember().getId())
				.fromMemberLabel(s.getFromMember().getDisplayName())
				.toMemberId(s.getToMember().getId())
				.toMemberLabel(s.getToMember().getDisplayName())
				.amount(s.getAmount())
				.note(s.getNote())
				.recordedByUserId(s.getRecordedBy().getId())
				.createdAt(s.getCreatedAt())
				.build();
	}

	public SettlementSuggestionResponse toSuggestion(SettlementSuggestion s) {
		return SettlementSuggestionResponse.builder()
				.fromMemberId(s.getFromMemberId())
				.fromMemberLabel(s.getFromMemberLabel())
				.toMemberId(s.getToMemberId())
				.toMemberLabel(s.getToMemberLabel())
				.amount(s.getAmount())
				.build();
	}

	public NotificationResponse toNotification(InAppNotification n) {
		Trip trip = n.getTrip();
		return NotificationResponse.builder()
				.id(n.getId())
				.type(n.getType())
				.title(n.getTitle())
				.message(n.getMessage())
				.tripId(trip != null ? trip.getId() : null)
				.tripTitle(trip != null ? trip.getTitle() : null)
				.read(n.getReadAt() != null)
				.createdAt(n.getCreatedAt())
				.build();
	}

	public PaymentRequestResponse toPaymentRequest(PaymentRequest p) {
		return PaymentRequestResponse.builder()
				.id(p.getId())
				.tripId(p.getTrip().getId())
				.creditorMemberId(p.getCreditorMember().getId())
				.creditorLabel(p.getCreditorMember().getDisplayName())
				.debtorMemberId(p.getDebtorMember().getId())
				.debtorLabel(p.getDebtorMember().getDisplayName())
				.amount(p.getAmount())
				.note(p.getNote())
				.status(p.getStatus())
				.createdAt(p.getCreatedAt())
				.build();
	}
}
