package com.tripTally.dto.auth;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthResponse {

	String token;
	UserResponse user;
}
