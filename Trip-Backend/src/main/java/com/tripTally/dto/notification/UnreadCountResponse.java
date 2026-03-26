package com.tripTally.dto.notification;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UnreadCountResponse {

	long count;
}
