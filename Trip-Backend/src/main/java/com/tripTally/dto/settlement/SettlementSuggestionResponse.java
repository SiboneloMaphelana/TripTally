package com.tripTally.dto.settlement;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SettlementSuggestionResponse {

	Long fromMemberId;
	String fromMemberLabel;
	Long toMemberId;
	String toMemberLabel;
	BigDecimal amount;
}
