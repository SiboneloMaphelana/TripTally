package com.triptally.repository;

import com.triptally.domain.entity.Expense;
import com.triptally.domain.entity.Trip;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

	Optional<Expense> findByIdAndTrip(Long id, Trip trip);

	long countByTrip(Trip trip);

	@EntityGraph(attributePaths = {"payer", "participants", "participants.tripMember"})
	@Query("select e from Expense e where e.trip = :trip")
	List<Expense> findAllForLedger(@Param("trip") Trip trip);

	@Override
	@EntityGraph(attributePaths = {"payer", "participants", "participants.tripMember"})
	Page<Expense> findAll(Specification<Expense> spec, Pageable pageable);
}
