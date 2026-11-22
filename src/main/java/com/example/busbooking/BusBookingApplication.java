package com.example.busbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

import java.util.TimeZone;

// API identity is loaded by our JWT filter; do not create Boot's fallback user/password.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class BusBookingApplication {

	public static void main(String[] args) {
		// JDBC sends the JVM timezone before Hikari connection-init-sql can run.
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
		SpringApplication.run(BusBookingApplication.class, args);
	}

}
