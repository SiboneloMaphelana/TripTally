package com.tripTally.dto.notification;

import com.tripTally.domain.entity.NotificationType;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NotificationResponse {

	Long id;
	NotificationType type;
	String title;
	String message;
	Long tripId;
	String tripTitle;
	boolean read;
	Instant createdAt;
}
