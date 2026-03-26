package com.tripTally;

import com.tripTally.config.TriptallyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(TriptallyProperties.class)
public class TripTallyApplication {

	public static void main(String[] args) {
		SpringApplication.run(TripTallyApplication.class, args);
	}
}
