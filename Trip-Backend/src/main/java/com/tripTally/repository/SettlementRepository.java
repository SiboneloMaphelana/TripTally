package com.tripTally.repository;

import com.tripTally.domain.entity.Settlement;
import com.tripTally.domain.entity.Trip;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

	List<Settlement> findByTripOrderByCreatedAtDesc(Trip trip);
}
