package com.triptally.service;

import com.triptally.domain.entity.Expense;
import com.triptally.domain.entity.ExpenseParticipant;
import com.triptally.domain.entity.Settlement;
import com.triptally.domain.entity.Trip;
import com.triptally.domain.entity.TripMember;
import com.triptally.domain.entity.User;
import com.triptally.dto.balance.MemberBalanceResponse;
import com.triptally.dto.balance.TripBalancesResponse;
import com.triptally.dto.settlement.SettlementSuggestionResponse;
import com.triptally.mapper.DtoMapper;
import com.triptally.repository.ExpenseRepository;
import com.triptally.repository.SettlementRepository;
import com.triptally.repository.TripMemberRepository;
import com.triptally.service.settlement.SettlementSimplifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripLedgerService {

	private static final int SCALE = 2;
	private static final RoundingMode RM = RoundingMode.HALF_UP;

	private final TripAccessService tripAccessService;
	private final TripMemberRepository tripMemberRepository;
	private final ExpenseRepository expenseRepository;
	private final SettlementRepository settlementRepository;
	private final SettlementSimplifier settlementSimplifier;
	private final DtoMapper dtoMapper;

	public TripLedgerService(
			TripAccessService tripAccessService,
			TripMemberRepository tripMemberRepository,
			ExpenseRepository expenseRepository,
			SettlementRepository settlementRepository,
			SettlementSimplifier settlementSimplifier,
			DtoMapper dtoMapper) {
		this.tripAccessService = tripAccessService;
		this.tripMemberRepository = tripMemberRepository;
		this.expenseRepository = expenseRepository;
		this.settlementRepository = settlementRepository;
		this.settlementSimplifier = settlementSimplifier;
		this.dtoMapper = dtoMapper;
	}

	@Transactional(readOnly = true)
	public TripBalancesResponse balances(Long tripId, User user) {
		Trip trip = tripAccessService.requireTripMember(tripId, user);
		LedgerSnapshot snap = computeSnapshot(trip);
		List<MemberBalanceResponse> rows = tripMemberRepository.findByTripOrderByCreatedAtAsc(trip).stream()
				.map(m -> dtoMapper.toBalance(
						m,
						snap.paid().getOrDefault(m.getId(), BigDecimal.ZERO),
						snap.owed().getOrDefault(m.getId(), BigDecimal.ZERO),
						snap.net().getOrDefault(m.getId(), BigDecimal.ZERO)))
				.toList();
		return TripBalancesResponse.builder()
				.currencyCode(trip.getCurrencyCode())
				.members(rows)
				.build();
	}

	@Transactional(readOnly = true)
	public List<SettlementSuggestionResponse> suggestions(Long tripId, User user) {
		Trip trip = tripAccessService.requireTripMember(tripId, user);
		LedgerSnapshot snap = computeSnapshot(trip);
		Map<Long, String> labels = tripMemberRepository.findByTripOrderByCreatedAtAsc(trip).stream()
				.collect(java.util.stream.Collectors.toMap(TripMember::getId, TripMember::getDisplayName));
		return settlementSimplifier.simplify(snap.net(), labels).stream()
				.map(dtoMapper::toSuggestion)
				.toList();
	}

	private LedgerSnapshot computeSnapshot(Trip trip) {
		List<TripMember> members = tripMemberRepository.findByTripOrderByCreatedAtAsc(trip);
		Map<Long, BigDecimal> paid = new HashMap<>();
		Map<Long, BigDecimal> owed = new HashMap<>();
		for (TripMember m : members) {
			paid.put(m.getId(), BigDecimal.ZERO.setScale(SCALE, RM));
			owed.put(m.getId(), BigDecimal.ZERO.setScale(SCALE, RM));
		}
		List<Expense> expenses = expenseRepository.findAllForLedger(trip);
		for (Expense e : expenses) {
			paid.merge(e.getPayer().getId(), e.getAmount(), BigDecimal::add);
			for (ExpenseParticipant p : e.getParticipants()) {
				owed.merge(p.getTripMember().getId(), p.getOwedAmount(), BigDecimal::add);
			}
		}
		Map<Long, BigDecimal> net = new HashMap<>();
		for (TripMember m : members) {
			BigDecimal p = paid.getOrDefault(m.getId(), BigDecimal.ZERO).setScale(SCALE, RM);
			BigDecimal o = owed.getOrDefault(m.getId(), BigDecimal.ZERO).setScale(SCALE, RM);
			net.put(m.getId(), p.subtract(o).setScale(SCALE, RM));
		}
		List<Settlement> settlements = settlementRepository.findByTripOrderByCreatedAtDesc(trip);
		for (Settlement s : settlements) {
			net.merge(s.getFromMember().getId(), s.getAmount(), BigDecimal::add);
			net.merge(s.getToMember().getId(), s.getAmount(), BigDecimal::subtract);
		}
		for (TripMember m : members) {
			net.put(m.getId(), net.getOrDefault(m.getId(), BigDecimal.ZERO).setScale(SCALE, RM));
		}
		return new LedgerSnapshot(scaleMoneyMap(paid), scaleMoneyMap(owed), scaleMoneyMap(net));
	}

	private Map<Long, BigDecimal> scaleMoneyMap(Map<Long, BigDecimal> in) {
		Map<Long, BigDecimal> out = new HashMap<>();
		in.forEach((k, v) -> out.put(k, v.setScale(SCALE, RM)));
		return out;
	}

	private record LedgerSnapshot(Map<Long, BigDecimal> paid, Map<Long, BigDecimal> owed, Map<Long, BigDecimal> net) {
	}
}
