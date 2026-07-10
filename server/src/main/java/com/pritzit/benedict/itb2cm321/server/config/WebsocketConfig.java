package com.pritzit.benedict.itb2cm321.server.config;

import com.pritzit.benedict.itb2cm321.server.components.Websocket;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for the server application.
 * Enables WebSocket support and registers the WebSocket handler at the /ws endpoint.
 * Allows connections from all origins.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebsocketConfig implements WebSocketConfigurer {
    private final Websocket websocket;

    /**
     * Registers WebSocket handlers with the specified endpoint.
     * Configures the /ws endpoint to accept connections from any origin.
     * @param registry The WebSocket handler registry
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
//        registry.addHandler(new SynAckMiddleware(websocket), "/ws").setAllowedOrigins("*");
        registry.addHandler(websocket, "/ws").setAllowedOrigins("*");
    }
}
