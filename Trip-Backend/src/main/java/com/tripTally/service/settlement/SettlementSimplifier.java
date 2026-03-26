package com.triptally.service.settlement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SettlementSimplifier {

	private static final int SCALE = 2;
	private static final RoundingMode RM = RoundingMode.HALF_UP;

	public List<SettlementSuggestion> simplify(
			Map<Long, BigDecimal> netByMemberId,
			Map<Long, String> memberLabels) {
		Map<Long, BigDecimal> balances = new HashMap<>();
		netByMemberId.forEach((id, v) -> balances.put(id, v.setScale(SCALE, RM)));

		List<SettlementSuggestion> suggestions = new ArrayList<>();

		while (true) {
			Long debtorId = null;
			BigDecimal debt = BigDecimal.ZERO;
			for (Map.Entry<Long, BigDecimal> e : balances.entrySet()) {
				if (e.getValue().compareTo(BigDecimal.ZERO) < 0) {
					if (debtorId == null || e.getValue().compareTo(debt) < 0) {
						debtorId = e.getKey();
						debt = e.getValue();
					}
				}
			}
			if (debtorId == null) {
				break;
			}

			Long creditorId = null;
			BigDecimal credit = BigDecimal.ZERO;
			for (Map.Entry<Long, BigDecimal> e : balances.entrySet()) {
				if (e.getValue().compareTo(BigDecimal.ZERO) > 0) {
					if (creditorId == null || e.getValue().compareTo(credit) > 0) {
						creditorId = e.getKey();
						credit = e.getValue();
					}
				}
			}
			if (creditorId == null) {
				break;
			}

			BigDecimal payAmount = debt.abs().min(credit).setScale(SCALE, RM);
			if (payAmount.compareTo(new BigDecimal("0.01")) < 0) {
				break;
			}

			suggestions.add(SettlementSuggestion.builder()
					.fromMemberId(debtorId)
					.fromMemberLabel(memberLabels.getOrDefault(debtorId, "?"))
					.toMemberId(creditorId)
					.toMemberLabel(memberLabels.getOrDefault(creditorId, "?"))
					.amount(payAmount)
					.build());

			balances.put(debtorId, balances.get(debtorId).add(payAmount));
			balances.put(creditorId, balances.get(creditorId).subtract(payAmount));
		}

		suggestions.sort(Comparator.comparing(SettlementSuggestion::getAmount).reversed());
		return suggestions;
	}
}
