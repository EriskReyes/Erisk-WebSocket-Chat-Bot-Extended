package com.pritzit.benedict.itb2cm321.server.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration class enabling scheduled task execution.
 * Allows components to use @Scheduled annotations for periodic task execution.
 * Used for periodic session information broadcasts.
 */
@Slf4j
@Configuration
@EnableScheduling
public class SchedulingConfig {

    /**
     * Logs initialization message when configuration is loaded.
     */
    @PostConstruct
    public void printConfiguration() {
        log.info("SchedulingConfig initialized");
    }
}
