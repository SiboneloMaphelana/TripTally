package com.tripTally.dto.auth;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserResponse {

	Long id;
	String email;
	String displayName;
}
