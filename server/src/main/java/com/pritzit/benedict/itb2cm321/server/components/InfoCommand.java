package com.pritzit.benedict.itb2cm321.server.components;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Command for providing client information.
 * Returns the client name and session ID.
 */
@Slf4j
@Component
public class InfoCommand {

    /**
     * Executes the info command to retrieve client information.
     * @param clientName The name of the client
     * @param sessionId The session ID of the client
     * @return Formatted client information string
     */
    public String execute(String clientName, String sessionId) {
        log.debug("Executing info command for client: {} with session: {}", clientName, sessionId);
        return String.format("Client: %s | Session-ID: %s", clientName, sessionId);
    }
}
