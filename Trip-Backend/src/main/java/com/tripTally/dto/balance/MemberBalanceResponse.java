package com.tripTally.dto.balance;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MemberBalanceResponse {

	Long tripMemberId;
	String label;
	BigDecimal totalPaid;
	BigDecimal totalOwed;
	BigDecimal netBalance;
}
