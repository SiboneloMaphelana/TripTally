package com.triptally.dto.member;

import com.triptally.domain.entity.TripMemberRole;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TripMemberResponse {

	Long id;
	String displayName;
	String invitedEmail;
	TripMemberRole role;
	Long linkedUserId;
}
