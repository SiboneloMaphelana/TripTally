package com.triptally.service.split;

import com.triptally.domain.entity.SplitMode;
import com.triptally.exception.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ExpenseSplitCalculator {

	private static final int MONEY_SCALE = 2;
	private static final RoundingMode RM = RoundingMode.HALF_UP;

	public List<CalculatedOwed> calculate(SplitMode mode, BigDecimal amount, List<SplitParticipantInput> participants) {
		if (participants == null || participants.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "At least one participant is required");
		}
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Amount must be positive");
		}
		return switch (mode) {
			case EQUAL -> equalSplit(amount, participants);
			case EXACT -> exactSplit(amount, participants);
			case PERCENTAGE -> percentageSplit(amount, participants);
			case SHARES -> shareSplit(amount, participants);
		};
	}

	List<CalculatedOwed> equalSplit(BigDecimal amount, List<SplitParticipantInput> participants) {
		int n = participants.size();
		BigDecimal each = amount.divide(BigDecimal.valueOf(n), MONEY_SCALE, RoundingMode.DOWN);
		BigDecimal allocated = BigDecimal.ZERO;
		List<CalculatedOwed> out = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			SplitParticipantInput p = participants.get(i);
			BigDecimal owed;
			if (i == n - 1) {
				owed = amount.subtract(allocated);
			}
			else {
				owed = each;
				allocated = allocated.add(each);
			}
			out.add(CalculatedOwed.builder()
					.tripMemberId(p.getTripMemberId())
					.owedAmount(owed)
					.splitInputStored(BigDecimal.ONE)
					.build());
		}
		return out;
	}

	List<CalculatedOwed> exactSplit(BigDecimal amount, List<SplitParticipantInput> participants) {
		BigDecimal sum = BigDecimal.ZERO;
		List<CalculatedOwed> out = new ArrayList<>();
		for (SplitParticipantInput p : participants) {
			if (p.getSplitInput() == null) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Exact split requires an amount for each participant");
			}
			BigDecimal part = p.getSplitInput().setScale(MONEY_SCALE, RM);
			if (part.compareTo(BigDecimal.ZERO) < 0) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Split amounts cannot be negative");
			}
			sum = sum.add(part);
			out.add(CalculatedOwed.builder()
					.tripMemberId(p.getTripMemberId())
					.owedAmount(part)
					.splitInputStored(part)
					.build());
		}
		if (sum.compareTo(amount.setScale(MONEY_SCALE, RM)) != 0) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Exact split amounts must equal the expense total");
		}
		return out;
	}

	List<CalculatedOwed> percentageSplit(BigDecimal amount, List<SplitParticipantInput> participants) {
		BigDecimal totalPct = BigDecimal.ZERO;
		for (SplitParticipantInput p : participants) {
			if (p.getSplitInput() == null) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Percentage split requires a percent for each participant");
			}
			if (p.getSplitInput().compareTo(BigDecimal.ZERO) < 0) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Percentages cannot be negative");
			}
			totalPct = totalPct.add(p.getSplitInput());
		}
		if (totalPct.setScale(2, RM).compareTo(new BigDecimal("100.00")) != 0) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Percentages must total 100");
		}
		List<CalculatedOwed> out = new ArrayList<>();
		BigDecimal allocated = BigDecimal.ZERO;
		int n = participants.size();
		for (int i = 0; i < n; i++) {
			SplitParticipantInput p = participants.get(i);
			BigDecimal pct = p.getSplitInput();
			BigDecimal owed;
			if (i == n - 1) {
				owed = amount.subtract(allocated).setScale(MONEY_SCALE, RM);
			}
			else {
				owed = amount.multiply(pct).divide(new BigDecimal("100"), MONEY_SCALE, RM);
				allocated = allocated.add(owed);
			}
			out.add(CalculatedOwed.builder()
					.tripMemberId(p.getTripMemberId())
					.owedAmount(owed)
					.splitInputStored(p.getSplitInput())
					.build());
		}
		return out;
	}

	List<CalculatedOwed> shareSplit(BigDecimal amount, List<SplitParticipantInput> participants) {
		BigDecimal totalShares = BigDecimal.ZERO;
		for (SplitParticipantInput p : participants) {
			if (p.getSplitInput() == null || p.getSplitInput().compareTo(BigDecimal.ZERO) <= 0) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Share split requires a positive weight for each participant");
			}
			totalShares = totalShares.add(p.getSplitInput());
		}
		List<CalculatedOwed> out = new ArrayList<>();
		BigDecimal allocated = BigDecimal.ZERO;
		int n = participants.size();
		for (int i = 0; i < n; i++) {
			SplitParticipantInput p = participants.get(i);
			BigDecimal owed;
			if (i == n - 1) {
				owed = amount.subtract(allocated).setScale(MONEY_SCALE, RM);
			}
			else {
				owed = amount.multiply(p.getSplitInput()).divide(totalShares, MONEY_SCALE, RM);
				allocated = allocated.add(owed);
			}
			out.add(CalculatedOwed.builder()
					.tripMemberId(p.getTripMemberId())
					.owedAmount(owed)
					.splitInputStored(p.getSplitInput())
					.build());
		}
		return out;
	}
}
