package com.triptally.service;

import com.triptally.domain.entity.Trip;
import com.triptally.domain.entity.TripMember;
import com.triptally.domain.entity.TripMemberRole;
import com.triptally.domain.entity.User;
import com.triptally.dto.member.TripMemberCreateRequest;
import com.triptally.dto.member.TripMemberResponse;
import com.triptally.exception.ApiException;
import com.triptally.mapper.DtoMapper;
import com.triptally.repository.TripMemberRepository;
import com.triptally.repository.UserRepository;
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
