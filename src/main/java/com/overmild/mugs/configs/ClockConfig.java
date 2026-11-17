package com.overmild.mugs.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    /** All timestamps are UTC. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** Zone used only where a local calendar date is needed, such as a fill's "today". */
    @Bean
    public ZoneId localZone(@Value("${app.time.local-zone}") String zone) {
        return ZoneId.of(zone);
    }
}
