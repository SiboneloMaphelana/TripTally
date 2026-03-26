package com.tripTally.dto.settlement;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class SettlementCreateRequest {

	@NotNull
	private Long fromMemberId;

	@NotNull
	private Long toMemberId;

	@NotNull
	@DecimalMin(value = "0.01", inclusive = true)
	private BigDecimal amount;

	@Size(max = 500)
	private String note;
}
