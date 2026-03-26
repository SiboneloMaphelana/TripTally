package com.triptally.service.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SettlementSimplifierTest {

	private final SettlementSimplifier simplifier = new SettlementSimplifier();

	@Test
	void simplifiesToMinimumTransfers() {
		Map<Long, BigDecimal> net = new HashMap<>();
		net.put(1L, new BigDecimal("120.00"));
		net.put(2L, new BigDecimal("-70.00"));
		net.put(3L, new BigDecimal("-50.00"));
		Map<Long, String> labels = Map.of(1L, "A", 2L, "B", 3L, "C");
		List<SettlementSuggestion> out = simplifier.simplify(net, labels);
		assertThat(out).hasSize(2);
		BigDecimal total = out.stream().map(SettlementSuggestion::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		assertThat(total).isEqualByComparingTo("120.00");
	}
}
