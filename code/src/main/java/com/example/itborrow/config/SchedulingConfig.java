package com.example.itborrow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Scheduling remains enabled when either background feature is independently disabled. */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
