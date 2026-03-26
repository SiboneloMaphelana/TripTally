package com.triptally.service;

import com.triptally.domain.entity.User;
import com.triptally.dto.auth.AuthResponse;
import com.triptally.dto.auth.LoginRequest;
import com.triptally.dto.auth.RegisterRequest;
import com.triptally.dto.auth.UserResponse;
import com.triptally.exception.ApiException;
import com.triptally.mapper.DtoMapper;
import com.triptally.repository.UserRepository;
import com.triptally.security.JwtService;
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
}
