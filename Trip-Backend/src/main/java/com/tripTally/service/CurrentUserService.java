package com.tripTally.service;

import com.tripTally.domain.entity.User;
import com.tripTally.exception.ApiException;
import com.tripTally.repository.UserRepository;
import com.tripTally.security.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

	private final UserRepository userRepository;

	public CurrentUserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public User requireUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AppUserDetails details)) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
		}
		return userRepository.findById(details.getUser().getId())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
	}
}
