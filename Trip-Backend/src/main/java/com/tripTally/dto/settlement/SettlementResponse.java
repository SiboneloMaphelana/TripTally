package com.triptally.dto.settlement;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SettlementResponse {

	Long id;
	Long fromMemberId;
	String fromMemberLabel;
	Long toMemberId;
	String toMemberLabel;
	BigDecimal amount;
	String note;
	Long recordedByUserId;
	Instant createdAt;
}
