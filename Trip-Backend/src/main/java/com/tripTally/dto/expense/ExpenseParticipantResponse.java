package com.tripTally.dto.expense;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExpenseParticipantResponse {

	Long tripMemberId;
	String memberLabel;
	BigDecimal owedAmount;
	BigDecimal splitInput;
}
