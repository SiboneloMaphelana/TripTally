package com.tripTally.service;

import com.tripTally.domain.entity.InAppNotification;
import com.tripTally.domain.entity.NotificationType;
import com.tripTally.domain.entity.Trip;
import com.tripTally.domain.entity.User;
import com.tripTally.dto.notification.NotificationResponse;
import com.tripTally.dto.notification.UnreadCountResponse;
import com.tripTally.exception.ApiException;
import com.tripTally.mapper.DtoMapper;
import com.tripTally.repository.InAppNotificationRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

	private final InAppNotificationRepository notificationRepository;
	private final DtoMapper dtoMapper;

	public NotificationService(InAppNotificationRepository notificationRepository, DtoMapper dtoMapper) {
		this.notificationRepository = notificationRepository;
		this.dtoMapper = dtoMapper;
	}

	@Transactional
	public void notify(
			User recipient,
			NotificationType type,
			String title,
			String message,
			Trip trip) {
		InAppNotification n = InAppNotification.builder()
				.recipient(recipient)
				.type(type)
				.title(title)
				.message(message)
				.trip(trip)
				.build();
		notificationRepository.save(n);
	}

	@Transactional(readOnly = true)
	public List<NotificationResponse> listForUser(User user) {
		return notificationRepository.findTop50ByRecipient_IdOrderByCreatedAtDesc(user.getId()).stream()
				.map(dtoMapper::toNotification)
				.toList();
	}

	@Transactional(readOnly = true)
	public UnreadCountResponse unreadCount(User user) {
		return UnreadCountResponse.builder()
				.count(notificationRepository.countByRecipient_IdAndReadAtIsNull(user.getId()))
				.build();
	}

	@Transactional
	public void markRead(Long notificationId, User user) {
		InAppNotification n = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
		if (!n.getRecipient().getId().equals(user.getId())) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Not your notification");
		}
		if (n.getReadAt() == null) {
			n.setReadAt(Instant.now());
		}
	}
}
