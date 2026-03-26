package com.triptally.dto.trip;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TripResponse {

	Long id;
	String title;
	String destination;
	LocalDate startDate;
	LocalDate endDate;
	String coverImagePath;
	String currencyCode;
	Long ownerUserId;
}
