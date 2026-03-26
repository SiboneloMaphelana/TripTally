package com.tripTally.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileUpdateResponse {

	UserResponse user;

	/** Present only when the email changed; clients must replace their stored JWT. */
	String token;
}
