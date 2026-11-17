package com.overmild.mugs.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /** All timestamps are UTC; clients convert to local time. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
