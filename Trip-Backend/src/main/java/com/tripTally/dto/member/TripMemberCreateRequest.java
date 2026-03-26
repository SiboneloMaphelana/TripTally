package com.tripTally.dto.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TripMemberCreateRequest {

	@Size(max = 120)
	private String displayName;

	@Email
	@Size(max = 255)
	private String invitedEmail;

	public boolean isPlaceholder() {
		return invitedEmail == null || invitedEmail.isBlank();
	}
}
