package com.tripTally.dto.payment;

import com.tripTally.domain.entity.PaymentRequestStatus;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PaymentRequestResponse {

	Long id;
	Long tripId;
	Long creditorMemberId;
	String creditorLabel;
	Long debtorMemberId;
	String debtorLabel;
	BigDecimal amount;
	String note;
	PaymentRequestStatus status;
	Instant createdAt;
}
