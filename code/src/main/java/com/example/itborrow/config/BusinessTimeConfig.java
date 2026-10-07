package com.example.itborrow.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusinessTimeConfig {
    @Bean
    public Clock businessClock(@Value("${app.business-zone:Asia/Bangkok}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
