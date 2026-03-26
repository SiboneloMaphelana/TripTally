package com.triptally.repository;

import com.triptally.domain.entity.Trip;
import com.triptally.domain.entity.TripMember;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripMemberRepository extends JpaRepository<TripMember, Long> {

	List<TripMember> findByTripOrderByCreatedAtAsc(Trip trip);

	Optional<TripMember> findByIdAndTrip(Long id, Trip trip);

	boolean existsByTripAndUser_Id(Trip trip, Long userId);

	List<TripMember> findByTripAndIdIn(Trip trip, Collection<Long> ids);
}
