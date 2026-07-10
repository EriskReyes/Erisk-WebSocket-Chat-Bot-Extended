package com.pritzit.benedict.itb2cm321.server.models;

import lombok.*;
import org.springframework.web.socket.WebSocketSession;

/**
 * Represents a WebSocket session with associated client information.
 * Combines the client's name with their WebSocket connection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {
    /** The client's display name */
    private String name;

    /** The underlying WebSocket session */
    private WebSocketSession session;
}
