package com.tripTally.dto.trip;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

@Data
public class TripCreateRequest {

	@NotBlank
	@Size(max = 200)
	private String title;

	@NotBlank
	@Size(max = 300)
	private String destination;

	@NotNull
	private LocalDate startDate;

	@NotNull
	private LocalDate endDate;

	@NotBlank
	@Size(min = 3, max = 3)
	private String currencyCode;
}
