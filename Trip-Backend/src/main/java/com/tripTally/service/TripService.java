package com.tripTally.service;

import com.tripTally.domain.entity.Trip;
import com.tripTally.domain.entity.TripMember;
import com.tripTally.domain.entity.TripMemberRole;
import com.tripTally.domain.entity.User;
import com.tripTally.dto.trip.TripCreateRequest;
import com.tripTally.dto.trip.TripResponse;
import com.tripTally.dto.trip.TripUpdateRequest;
import com.tripTally.exception.ApiException;
import com.tripTally.mapper.DtoMapper;
import com.tripTally.repository.TripMemberRepository;
import com.tripTally.repository.TripRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripService {

	private final TripRepository tripRepository;
	private final TripMemberRepository tripMemberRepository;
	private final TripAccessService tripAccessService;
	private final DtoMapper dtoMapper;

	public TripService(
			TripRepository tripRepository,
			TripMemberRepository tripMemberRepository,
			TripAccessService tripAccessService,
			DtoMapper dtoMapper) {
		this.tripRepository = tripRepository;
		this.tripMemberRepository = tripMemberRepository;
		this.tripAccessService = tripAccessService;
		this.dtoMapper = dtoMapper;
	}

	@Transactional(readOnly = true)
	public List<TripResponse> listForUser(User user) {
		return tripRepository.findAllVisibleForUser(user.getId()).stream()
				.map(dtoMapper::toTrip)
				.toList();
	}

	@Transactional
	public TripResponse create(User owner, TripCreateRequest request) {
		if (request.getEndDate().isBefore(request.getStartDate())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "End date must be on or after start date");
		}
		Trip trip = Trip.builder()
				.owner(owner)
				.title(request.getTitle().trim())
				.destination(request.getDestination().trim())
				.startDate(request.getStartDate())
				.endDate(request.getEndDate())
				.currencyCode(request.getCurrencyCode().trim().toUpperCase())
				.build();
		trip = tripRepository.save(trip);
		TripMember ownerRow = TripMember.builder()
				.trip(trip)
				.user(owner)
				.displayName(owner.getDisplayName())
				.role(TripMemberRole.OWNER)
				.build();
		tripMemberRepository.save(ownerRow);
		return dtoMapper.toTrip(trip);
	}

	@Transactional(readOnly = true)
	public TripResponse get(Long tripId, User user) {
		Trip trip = tripAccessService.requireTripMember(tripId, user);
		return dtoMapper.toTrip(trip);
	}

	@Transactional
	public TripResponse update(Long tripId, User user, TripUpdateRequest request) {
		Trip trip = tripAccessService.requireTripMember(tripId, user);
		tripAccessService.requireTripOwner(trip, user);
		if (request.getEndDate().isBefore(request.getStartDate())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "End date must be on or after start date");
		}
		trip.setTitle(request.getTitle().trim());
		trip.setDestination(request.getDestination().trim());
		trip.setStartDate(request.getStartDate());
		trip.setEndDate(request.getEndDate());
		trip.setCoverImagePath(request.getCoverImagePath() != null ? request.getCoverImagePath().trim() : null);
		trip.setCurrencyCode(request.getCurrencyCode().trim().toUpperCase());
		tripRepository.save(trip);
		return dtoMapper.toTrip(trip);
	}

	@Transactional
	public void delete(Long tripId, User user) {
		Trip trip = tripAccessService.requireTripMember(tripId, user);
		tripAccessService.requireTripOwner(trip, user);
		tripRepository.delete(trip);
	}
}
