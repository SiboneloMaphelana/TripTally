package com.tripTally.dto.expense;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ExpenseParticipantRequest {

	@NotNull
	private Long tripMemberId;

	private BigDecimal splitInput;
}
