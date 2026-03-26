package com.tripTally.service;

import com.tripTally.domain.entity.User;
import com.tripTally.dto.auth.AuthResponse;
import com.tripTally.dto.auth.ChangePasswordRequest;
import com.tripTally.dto.auth.LoginRequest;
import com.tripTally.dto.auth.ProfileUpdateRequest;
import com.tripTally.dto.auth.ProfileUpdateResponse;
import com.tripTally.dto.auth.RegisterRequest;
import com.tripTally.dto.auth.UserResponse;
import com.tripTally.exception.ApiException;
import com.tripTally.mapper.DtoMapper;
import com.tripTally.repository.UserRepository;
import com.tripTally.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;
	private final DtoMapper dtoMapper;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			AuthenticationManager authenticationManager,
			DtoMapper dtoMapper) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.authenticationManager = authenticationManager;
		this.dtoMapper = dtoMapper;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = request.getEmail().trim().toLowerCase();
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new ApiException(HttpStatus.CONFLICT, "That email is already registered");
		}
		User user = User.builder()
				.email(email)
				.passwordHash(passwordEncoder.encode(request.getPassword()))
				.displayName(request.getDisplayName().trim())
				.build();
		user = userRepository.save(user);
		String token = jwtService.generateToken(user);
		return AuthResponse.builder()
				.token(token)
				.user(dtoMapper.toUser(user))
				.build();
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		String email = request.getEmail().trim().toLowerCase();
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(email, request.getPassword()));
		User user = userRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
		String token = jwtService.generateToken(user);
		return AuthResponse.builder()
				.token(token)
				.user(dtoMapper.toUser(user))
				.build();
	}

	public UserResponse me(User user) {
		return dtoMapper.toUser(user);
	}

	@Transactional
	public ProfileUpdateResponse updateProfile(User current, ProfileUpdateRequest request) {
		User user = userRepository.findById(current.getId())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
		String newEmail = request.getEmail().trim().toLowerCase();
		if (!newEmail.equalsIgnoreCase(user.getEmail())
				&& userRepository.existsByEmailIgnoreCaseAndIdNot(newEmail, user.getId())) {
			throw new ApiException(HttpStatus.CONFLICT, "That email is already registered");
		}
		boolean emailChanged = !user.getEmail().equalsIgnoreCase(newEmail);
		user.setEmail(newEmail);
		user.setDisplayName(request.getDisplayName().trim());
		user = userRepository.save(user);
		UserResponse body = dtoMapper.toUser(user);
		String token = emailChanged ? jwtService.generateToken(user) : null;
		return ProfileUpdateResponse.builder()
				.user(body)
				.token(token)
				.build();
	}

	@Transactional
	public void changePassword(User current, ChangePasswordRequest request) {
		User user = userRepository.findById(current.getId())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
		if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
		}
		user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
		userRepository.save(user);
	}
}
