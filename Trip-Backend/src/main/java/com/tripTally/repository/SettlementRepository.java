package com.triptally.repository;

import com.triptally.domain.entity.Settlement;
import com.triptally.domain.entity.Trip;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

	List<Settlement> findByTripOrderByCreatedAtDesc(Trip trip);
}
