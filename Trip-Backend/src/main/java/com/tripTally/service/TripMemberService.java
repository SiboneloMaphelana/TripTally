package com.tripTally.service;

import com.tripTally.domain.entity.Trip;
import com.tripTally.domain.entity.TripMember;
import com.tripTally.domain.entity.TripMemberRole;
import com.tripTally.domain.entity.User;
import com.tripTally.dto.member.TripMemberCreateRequest;
import com.tripTally.dto.member.TripMemberResponse;
import com.tripTally.dto.member.TripMemberSelfResponse;
import com.tripTally.exception.ApiException;
import com.tripTally.mapper.DtoMapper;
import com.tripTally.repository.TripMemberRepository;
import com.tripTally.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripMemberService {

	private final TripAccessService tripAccessService;
	private final TripMemberRepository tripMemberRepository;
	private final UserRepository userRepository;
	private final DtoMapper dtoMapper;

	public TripMemberService(
			TripAccessService tripAccessService,
			TripMemberRepository tripMemberRepository,
			UserRepository userRepository,
			DtoMapper dtoMapper) {
		this.tripAccessService = tripAccessService;
		this.tripMemberRepository = tripMemberRepository;
		this.userRepository = userRepository;
		this.dtoMapper = dtoMapper;
	}

	@Transactional(readOnly = true)
	public List<TripMemberResponse> list(Long tripId, User user) {
		Trip trip = tripAccessService.requireTripMember(tripId, user);
		return tripMemberRepository.findByTripOrderByCreatedAtAsc(trip).stream()
				.map(dtoMapper::toMember)
				.toList();
	}

	@Transactional(readOnly = true)
	public TripMemberSelfResponse selfMember(Long tripId, User user) {
		Trip trip = tripAccessService.requireTripMember(tripId, user);
		TripMember m = tripMemberRepository
				.findByTripAndUser_Id(trip, user.getId())
				.orElseThrow(() -> new ApiException(
						HttpStatus.NOT_FOUND, "Your account is not linked to a traveler on this trip."));
		return TripMemberSelfResponse.builder().tripMemberId(m.getId()).build();
	}

	@Transactional
	public TripMemberResponse add(Long tripId, User actor, TripMemberCreateRequest request) {
		Trip trip = tripAccessService.requireTripMember(tripId, actor);
		tripAccessService.requireTripOwner(trip, actor);

		String invited = request.getInvitedEmail() != null ? request.getInvitedEmail().trim() : null;
		String name = request.getDisplayName() != null ? request.getDisplayName().trim() : null;

		if (invited != null && !invited.isEmpty()) {
			String email = invited.toLowerCase();
			if (tripMemberRepository.findByTripOrderByCreatedAtAsc(trip).stream()
					.anyMatch(m -> email.equalsIgnoreCase(trimToEmpty(m.getInvitedEmail())))) {
				throw new ApiException(HttpStatus.CONFLICT, "That email is already invited on this trip");
			}
			Optional<User> linkedUser = userRepository.findByEmailIgnoreCase(email);
			linkedUser.ifPresent(u -> {
				if (tripMemberRepository.existsByTripAndUser_Id(trip, u.getId())) {
					throw new ApiException(HttpStatus.CONFLICT, "That person is already on this trip");
				}
			});
			int at = email.indexOf('@');
			String display = (name != null && !name.isEmpty()) ? name : (at > 0 ? email.substring(0, at) : email);
			TripMember member = TripMember.builder()
					.trip(trip)
					.user(linkedUser.orElse(null))
					.displayName(display)
					.invitedEmail(email)
					.role(TripMemberRole.MEMBER)
					.build();
			member = tripMemberRepository.save(member);
			return dtoMapper.toMember(member);
		}

		if (name == null || name.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Add a name for your travel buddy, or invite by email");
		}
		TripMember placeholder = TripMember.builder()
				.trip(trip)
				.user(null)
				.displayName(name)
				.invitedEmail(null)
				.role(TripMemberRole.MEMBER)
				.build();
		placeholder = tripMemberRepository.save(placeholder);
		return dtoMapper.toMember(placeholder);
	}

	private static String trimToEmpty(String s) {
		return s == null ? "" : s.trim();
	}
}
