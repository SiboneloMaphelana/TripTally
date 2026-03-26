package com.triptally.dto.expense;

import com.triptally.domain.entity.ExpenseCategory;
import com.triptally.domain.entity.SplitMode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExpenseResponse {

	Long id;
	Long tripId;
	Long payerMemberId;
	String payerLabel;
	BigDecimal amount;
	ExpenseCategory category;
	String description;
	LocalDate expenseDate;
	SplitMode splitMode;
	boolean settled;
	boolean hasReceipt;
	List<ExpenseParticipantResponse> participants;
}
