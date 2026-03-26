package com.tripTally.controller;

import com.tripTally.dto.auth.AuthResponse;
import com.tripTally.dto.auth.ChangePasswordRequest;
import com.tripTally.dto.auth.LoginRequest;
import com.tripTally.dto.auth.ProfileUpdateRequest;
import com.tripTally.dto.auth.ProfileUpdateResponse;
import com.tripTally.dto.auth.RegisterRequest;
import com.tripTally.dto.auth.UserResponse;
import com.tripTally.service.AuthService;
import com.tripTally.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final CurrentUserService currentUserService;

	public AuthController(AuthService authService, CurrentUserService currentUserService) {
		this.authService = authService;
		this.currentUserService = currentUserService;
	}

	@PostMapping("/register")
	public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@GetMapping("/me")
	public UserResponse me() {
		return authService.me(currentUserService.requireUser());
	}

	@PatchMapping("/me")
	public ProfileUpdateResponse updateMe(@Valid @RequestBody ProfileUpdateRequest request) {
		return authService.updateProfile(currentUserService.requireUser(), request);
	}

	@PostMapping("/change-password")
	public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
		authService.changePassword(currentUserService.requireUser(), request);
	}
}
