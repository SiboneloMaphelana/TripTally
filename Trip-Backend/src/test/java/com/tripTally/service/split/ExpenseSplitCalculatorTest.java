package com.triptally.service.split;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.triptally.domain.entity.SplitMode;
import com.triptally.exception.ApiException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExpenseSplitCalculatorTest {

	private final ExpenseSplitCalculator calculator = new ExpenseSplitCalculator();

	@Test
	void equalSplit_distributesRemainderToLast() {
		var parts = List.of(
				SplitParticipantInput.builder().tripMemberId(1L).splitInput(BigDecimal.ZERO).build(),
				SplitParticipantInput.builder().tripMemberId(2L).splitInput(BigDecimal.ZERO).build(),
				SplitParticipantInput.builder().tripMemberId(3L).splitInput(BigDecimal.ZERO).build());
		List<CalculatedOwed> out = calculator.calculate(SplitMode.EQUAL, new BigDecimal("100.00"), parts);
		assertThat(out).hasSize(3);
		BigDecimal sum = out.stream().map(CalculatedOwed::getOwedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		assertThat(sum).isEqualByComparingTo("100.00");
	}

	@Test
	void exactSplit_rejectsWhenSumMismatch() {
		var parts = List.of(
				SplitParticipantInput.builder().tripMemberId(1L).splitInput(new BigDecimal("40.00")).build(),
				SplitParticipantInput.builder().tripMemberId(2L).splitInput(new BigDecimal("50.00")).build());
		assertThatThrownBy(() -> calculator.calculate(SplitMode.EXACT, new BigDecimal("100.00"), parts))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void exactSplit_acceptsMatchingTotal() {
		var parts = List.of(
				SplitParticipantInput.builder().tripMemberId(1L).splitInput(new BigDecimal("40.00")).build(),
				SplitParticipantInput.builder().tripMemberId(2L).splitInput(new BigDecimal("60.00")).build());
		List<CalculatedOwed> out = calculator.calculate(SplitMode.EXACT, new BigDecimal("100.00"), parts);
		assertThat(out.get(0).getOwedAmount()).isEqualByComparingTo("40.00");
		assertThat(out.get(1).getOwedAmount()).isEqualByComparingTo("60.00");
	}

	@Test
	void percentageSplit_mustTotalOneHundred() {
		var parts = List.of(
				SplitParticipantInput.builder().tripMemberId(1L).splitInput(new BigDecimal("50")).build(),
				SplitParticipantInput.builder().tripMemberId(2L).splitInput(new BigDecimal("40")).build());
		assertThatThrownBy(() -> calculator.calculate(SplitMode.PERCENTAGE, new BigDecimal("100.00"), parts))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void shareSplit_splitsByWeights() {
		var parts = List.of(
				SplitParticipantInput.builder().tripMemberId(1L).splitInput(new BigDecimal("1")).build(),
				SplitParticipantInput.builder().tripMemberId(2L).splitInput(new BigDecimal("3")).build());
		List<CalculatedOwed> out = calculator.calculate(SplitMode.SHARES, new BigDecimal("100.00"), parts);
		assertThat(out.get(0).getOwedAmount()).isEqualByComparingTo("25.00");
		assertThat(out.get(1).getOwedAmount()).isEqualByComparingTo("75.00");
	}
}
