package com.example.itborrow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class BusinessTimeConfig {
    @Bean
    public Clock businessClock(@Value("${app.business-zone:Asia/Bangkok}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
