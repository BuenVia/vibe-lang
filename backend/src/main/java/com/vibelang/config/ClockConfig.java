package com.vibelang.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /** Injected wherever "now" matters (e.g. spaced repetition), so tests can use a fixed clock. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
