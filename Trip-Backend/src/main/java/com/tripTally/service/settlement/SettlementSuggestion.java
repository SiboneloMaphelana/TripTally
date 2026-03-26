package com.triptally.service.settlement;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SettlementSuggestion {

	Long fromMemberId;
	String fromMemberLabel;
	Long toMemberId;
	String toMemberLabel;
	BigDecimal amount;
}
