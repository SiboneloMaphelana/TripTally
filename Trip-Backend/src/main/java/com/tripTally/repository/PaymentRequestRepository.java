package com.tripTally.repository;

import com.tripTally.domain.entity.PaymentRequest;
import com.tripTally.domain.entity.Trip;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {

	List<PaymentRequest> findByTripOrderByCreatedAtDesc(Trip trip);
}
