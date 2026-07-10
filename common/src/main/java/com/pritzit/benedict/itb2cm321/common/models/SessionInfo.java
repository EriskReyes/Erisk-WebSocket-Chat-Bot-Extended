package com.pritzit.benedict.itb2cm321.common.models;

import lombok.*;

/**
 * Data transfer object containing server session information.
 * Used to communicate the current number of connected clients to all clients.
 * Sent periodically by the server to keep clients informed of active connections.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfo {
    /** The number of currently connected clients */
    private int connectedClients;
}
