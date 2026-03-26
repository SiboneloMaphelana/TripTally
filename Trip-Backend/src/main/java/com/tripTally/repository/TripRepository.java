package com.triptally.repository;

import com.triptally.domain.entity.Trip;
import com.triptally.domain.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripRepository extends JpaRepository<Trip, Long> {

	List<Trip> findByOwnerOrderByCreatedAtDesc(User owner);

	@Query("""
			select distinct t from Trip t
			left join fetch t.owner
			where t.owner.id = :userId
			   or exists (
			     select 1 from TripMember m
			     where m.trip = t and m.user is not null and m.user.id = :userId
			   )
			order by t.createdAt desc
			""")
	List<Trip> findAllVisibleForUser(@Param("userId") Long userId);
}
