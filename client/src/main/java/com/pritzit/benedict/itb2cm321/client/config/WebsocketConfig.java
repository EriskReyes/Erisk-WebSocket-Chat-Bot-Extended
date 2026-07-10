package com.pritzit.benedict.itb2cm321.client.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration class for WebSocket client settings.
 * Reads connection URL and client name from application properties.
 * Provides configuration values to WebSocket connection components.
 */
@Getter
@Configuration
public class WebsocketConfig {
    @Value("${m321.websocket.url}")
    private String url = "ws://localhost:8080/ws";
    @Value("${spring.application.name}")
    private String name = "client";
}
