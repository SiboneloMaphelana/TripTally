package com.tripTally.dto.summary;

import com.tripTally.domain.entity.ExpenseCategory;
import com.tripTally.dto.expense.ExpenseResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TripSummaryResponse {

	String currencyCode;
	BigDecimal totalSpend;
	Map<ExpenseCategory, BigDecimal> spendByCategory;
	Map<Long, BigDecimal> spendByMember;
	List<ExpenseResponse> latestExpenses;
}
