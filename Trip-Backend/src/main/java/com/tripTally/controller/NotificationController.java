package com.tripTally.controller;

import com.tripTally.dto.notification.NotificationResponse;
import com.tripTally.dto.notification.UnreadCountResponse;
import com.tripTally.service.CurrentUserService;
import com.tripTally.service.NotificationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

	private final NotificationService notificationService;
	private final CurrentUserService currentUserService;

	public NotificationController(NotificationService notificationService, CurrentUserService currentUserService) {
		this.notificationService = notificationService;
		this.currentUserService = currentUserService;
	}

	@GetMapping
	public List<NotificationResponse> list() {
		return notificationService.listForUser(currentUserService.requireUser());
	}

	@GetMapping("/unread-count")
	public UnreadCountResponse unreadCount() {
		return notificationService.unreadCount(currentUserService.requireUser());
	}

	@PostMapping("/{id}/read")
	public void markRead(@PathVariable Long id) {
		notificationService.markRead(id, currentUserService.requireUser());
	}
}
