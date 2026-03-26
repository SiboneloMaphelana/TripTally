package com.tripTally.repository;

import com.tripTally.domain.entity.InAppNotification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {

	List<InAppNotification> findTop50ByRecipient_IdOrderByCreatedAtDesc(Long recipientId);

	long countByRecipient_IdAndReadAtIsNull(Long recipientId);
}
