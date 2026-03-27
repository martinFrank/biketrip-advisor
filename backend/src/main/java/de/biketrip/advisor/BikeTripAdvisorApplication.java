package de.biketrip.advisor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BikeTripAdvisorApplication {

    public static void main(String[] args) {
        SpringApplication.run(BikeTripAdvisorApplication.class, args);
    }
}
