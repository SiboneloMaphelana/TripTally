package com.tripTally.dto.expense;

import com.tripTally.domain.entity.ExpenseCategory;
import com.tripTally.domain.entity.SplitMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
public class ExpenseCreateRequest {

	@NotNull
	private Long payerMemberId;

	@NotNull
	@DecimalMin(value = "0.01", inclusive = true)
	private BigDecimal amount;

	@NotNull
	private ExpenseCategory category;

	@NotBlank
	@Size(max = 500)
	private String description;

	@NotNull
	private LocalDate expenseDate;

	@NotNull
	private SplitMode splitMode;

	private boolean settled;

	@NotNull
	@Valid
	private List<com.tripTally.dto.expense.ExpenseParticipantRequest> participants;
}
