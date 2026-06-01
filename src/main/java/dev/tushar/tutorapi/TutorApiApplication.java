package dev.tushar.tutorapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point. The {@link SpringBootApplication} meta-annotation enables
 * component scanning starting from this package, so every {@code @Service / @Repository
 * / @Controller / @Configuration} under {@code dev.tushar.tutorapi.*} is auto-discovered.
 *
 * <p>The active profile (dev / prod / test) is selected via
 * {@code spring.profiles.active=${SPRING_PROFILE:dev}}.
 */
@SpringBootApplication
public class TutorApiApplication {

    static void main(String[] args) {
        SpringApplication.run(TutorApiApplication.class, args);
    }

}
