package com.tripTally.service;

import com.tripTally.domain.entity.Settlement;
import com.tripTally.domain.entity.Trip;
import com.tripTally.domain.entity.TripMember;
import com.tripTally.domain.entity.User;
import com.tripTally.dto.settlement.SettlementCreateRequest;
import com.tripTally.dto.settlement.SettlementResponse;
import com.tripTally.exception.ApiException;
import com.tripTally.mapper.DtoMapper;
import com.tripTally.repository.SettlementRepository;
import com.tripTally.repository.TripMemberRepository;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementRecordingService {

	private final TripAccessService tripAccessService;
	private final SettlementRepository settlementRepository;
	private final TripMemberRepository tripMemberRepository;
	private final DtoMapper dtoMapper;

	public SettlementRecordingService(
			TripAccessService tripAccessService,
			SettlementRepository settlementRepository,
			TripMemberRepository tripMemberRepository,
			DtoMapper dtoMapper) {
		this.tripAccessService = tripAccessService;
		this.settlementRepository = settlementRepository;
		this.tripMemberRepository = tripMemberRepository;
		this.dtoMapper = dtoMapper;
	}

	@Transactional(readOnly = true)
	public List<SettlementResponse> list(Long tripId, User user) {
		Trip trip = tripAccessService.requireTripMember(tripId, user);
		return settlementRepository.findByTripOrderByCreatedAtDesc(trip).stream()
				.map(dtoMapper::toSettlement)
				.toList();
	}

	@Transactional
	public SettlementResponse record(Long tripId, User user, SettlementCreateRequest request) {
		Trip trip = tripAccessService.requireTripMember(tripId, user);
		TripMember actorMember = tripMemberRepository
				.findByTripAndUser_Id(trip, user.getId())
				.orElseThrow(() -> new ApiException(
						HttpStatus.FORBIDDEN,
						"You must be a linked traveler on this trip to record a payment."));
		TripMember from = loadMember(trip, request.getFromMemberId());
		TripMember to = loadMember(trip, request.getToMemberId());
		if (!from.getId().equals(actorMember.getId())) {
			throw new ApiException(
					HttpStatus.FORBIDDEN,
					"You can only record money you paid yourself. \"Paid by\" must be your traveler slot on this trip.");
		}
		if (from.getId().equals(to.getId())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Choose two different people for a settlement");
		}
		Settlement settlement = Settlement.builder()
				.trip(trip)
				.fromMember(from)
				.toMember(to)
				.amount(request.getAmount().setScale(2, RoundingMode.HALF_UP))
				.note(request.getNote() != null ? request.getNote().trim() : null)
				.recordedBy(user)
				.build();
		settlement = settlementRepository.save(settlement);
		return dtoMapper.toSettlement(settlement);
	}

	private TripMember loadMember(Trip trip, Long memberId) {
		return tripMemberRepository.findByIdAndTrip(memberId, trip)
				.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Members must belong to this trip"));
	}
}
