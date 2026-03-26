package com.tripTally.service;

import com.tripTally.domain.entity.Trip;
import com.tripTally.domain.entity.User;
import com.tripTally.exception.ApiException;
import com.tripTally.repository.TripMemberRepository;
import com.tripTally.repository.TripRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripAccessService {

	private final TripRepository tripRepository;
	private final TripMemberRepository tripMemberRepository;

	public TripAccessService(TripRepository tripRepository, TripMemberRepository tripMemberRepository) {
		this.tripRepository = tripRepository;
		this.tripMemberRepository = tripMemberRepository;
	}

	@Transactional(readOnly = true)
	public Trip requireTrip(Long tripId) {
		return tripRepository.findById(tripId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Trip not found"));
	}

	@Transactional(readOnly = true)
	public Trip requireTripMember(Long tripId, User user) {
		Trip trip = requireTrip(tripId);
		if (trip.getOwner().getId().equals(user.getId())) {
			return trip;
		}
		if (tripMemberRepository.existsByTripAndUser_Id(trip, user.getId())) {
			return trip;
		}
		throw new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this trip");
	}

	public void requireTripOwner(Trip trip, User user) {
		if (!trip.getOwner().getId().equals(user.getId())) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Only the trip owner can do this");
		}
	}
}
