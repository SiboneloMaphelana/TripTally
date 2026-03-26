package com.triptally.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "triptally")
public class TriptallyProperties {

	private final Jwt jwt = new Jwt();
	private final Storage storage = new Storage();

	@Getter
	@Setter
	public static class Jwt {
		private String secret;
		private long expirationMs;
	}

	@Getter
	@Setter
	public static class Storage {
		private String receiptsDir;
	}
}
