package com.pritzit.benedict.itb2cm321.server.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pritzit.benedict.itb2cm321.common.models.MessageType;
import com.pritzit.benedict.itb2cm321.common.models.SessionInfo;
import com.pritzit.benedict.itb2cm321.common.services.MessageConstructorService;
import com.pritzit.benedict.itb2cm321.server.models.Session;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
// START
// Added imports for new services
// END

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * WebSocket handler component managing client connections and message broadcasting.
 * Handles chat messages, server commands, and periodic session information updates.
 * Supports text filtering and command processing (math, info).
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class Websocket implements WebSocketHandler {

    private final ObjectMapper mapper;
    private final MessageConstructorService messageConstructorService;
    // Command components
    private final MathCommand mathCommand;
    private final InfoCommand infoCommand;
    private final TextFilter textFilter;

    private static ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    @PostConstruct
    public void printConfiguration() {
        log.info("Component: Websocket initialized");
    }

    /**
     * Scheduled task that logs session information and broadcasts it to all clients.
     * Runs every 5 seconds to keep clients updated with current session count.
     */
    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
    public void printMessage() {
        String sessionInfo = getSessionInfoOutput();
        log.info(sessionInfo);

        SessionInfo info = generateSessionInfo();

        String json = messageConstructorService.serializeSessionInfo(info);

        sendServerMessageToClients(json);
    }

    /**
     * Generates session information containing the current number of connected clients.
     * @return SessionInfo object with current connection count
     */
    private SessionInfo generateSessionInfo(){
        SessionInfo sessionInfo = SessionInfo.builder()
                .connectedClients(sessions.size())
                .build();

        log.debug("Generated session info: {}", sessionInfo);
        return sessionInfo;
    }

    /**
     * Generates and returns a formatted string containing information about active websocket sessions.
     *
     * The output includes the total number of active sessions and details of each session,
     * such as the session's name and ID.
     *
     * @return A formatted string summarizing the active websocket sessions managed by the application.
     */
    private String getSessionInfoOutput() {
        return String.format(
                """
                        +---------------------------+
                        | Websocket Session Manager |
                        +---------------------------+
                        Active Sessions: %1s
                        %2s
                        """
                , sessions.size(),
                sessions.values().stream()
                        .map(session -> "  - " + session.getName() + ": " + session.getSession().getId())
                        .collect(Collectors.joining("\n"))
        );
    }

    /**
     * This method is triggered after a WebSocket connection has been established. It processes the query parameters
     * from the WebSocket session's URI to extract the "name" parameter (if present) and logs the details of the connection.
     *
     * @param session the WebSocket session that has been established
     * @throws Exception if an error occurs during the processing of the connection initialization
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String query = session.getUri().getQuery();
        String name = null;

        if (query != null) {
            log.debug("Query: {}", query);

            // Query parameter nach & auftrennen
            String[] params = query.split("&");

            // Array auf "name=" filtern
            name = Arrays.stream(params).filter(p -> p.startsWith("name=")).findFirst().orElse(null);
            if (name != null) {
                name = name.replace("name=", "");
                log.debug("Name: {}", name);
            }
        }

        log.info("Websocket connected with name: {} and id: {}", name, session.getId());
        addSession(
                Session.builder()
                        .name(name != null ? name : "anonymous")
                        .session(session)
                        .build()
        );
    }

    /**
     * Handles incoming WebSocket messages from clients.
     * Processes server commands (starting with @server) and regular chat messages.
     * Applies text filtering to chat messages before broadcasting.
     * @param session The WebSocket session that sent the message
     * @param message The incoming message
     * @throws Exception If an error occurs during message processing
     */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = (String) message.getPayload();
        log.info("Websocket message received: {}", payload);

        try {
            // Deserialize the message to check if it's a server command
            com.pritzit.benedict.itb2cm321.common.models.Message parsedMessage =
                mapper.readValue(payload, com.pritzit.benedict.itb2cm321.common.models.Message.class);

            String messageContent = parsedMessage.getData().getMessage();

            // Check if message is a server command
            if (messageContent.startsWith("@server ")) {
                handleServerCommand(session, messageContent);
                return;
            }

            // Apply text filter to regular chat messages
            String filteredContent = textFilter.filterText(messageContent);

            // Update the message with filtered content
            parsedMessage.getData().setMessage(filteredContent);

            // Serialize back to JSON
            String filteredPayload = mapper.writeValueAsString(parsedMessage);
            WebSocketMessage<String> filteredMessage = new TextMessage(filteredPayload);
            broadcastMessage(filteredMessage, session.getId());
        } catch (JsonProcessingException e) {
            log.error("Error processing message from session {}: {}", session.getId(), e.getMessage());
            log.error("Invalid JSON payload: {}", payload);
            // Don't close connection - just ignore invalid messages
        } catch (Exception e) {
            log.error("Unexpected error handling message from session {}: {}", session.getId(), e.getMessage(), e);
            // Don't close connection
        }
    }

    /**
     * Handles server commands like @server math, @server info.
     * Intercepts commands starting with @server and redirects to appropriate Command class.
     */
    private void handleServerCommand(WebSocketSession session, String payload) {
        log.info("Processing server command: {}", payload);
        String command = payload.substring(8); // Remove "@server "
        String[] parts = command.split("\\s+", 2);
        String commandType = parts[0].toLowerCase();

        log.debug("Command type: {}, Parts: {}", commandType, String.join(" | ", parts));

        Optional<Session> sessionOpt = getSession(session.getId());
        String clientName = sessionOpt.map(Session::getName).orElse("anonymous");

        String response;

        // Redirect to appropriate Command class
        switch (commandType) {
            case "math" -> {
                if (parts.length < 2) {
                    response = "Error: No mathematical expression provided";
                } else {
                    // Use MathCommand to process the expression
                    log.info("Executing math command: {}", parts[1]);
                    response = mathCommand.execute(parts[1]);
                    log.info("Math result: {}", response);
                }
            }
            case "info" -> {
                // Use InfoCommand to get client information
                response = infoCommand.execute(clientName, session.getId());
            }
            default -> response = "Unknown command: " + commandType;
        }

        log.info("Sending response to client {}: {}", clientName, response);
        // Send response back to the requesting client
        sendServerResponseToClient(session, response);
    }

    /**
     * Sends a server response to a specific client
     */
    private void sendServerResponseToClient(WebSocketSession session, String response) {
        try {
            String json = messageConstructorService.generateMessageJson("server", null, MessageType.ServerInfo, response);
            WebSocketMessage<String> payload = new TextMessage(json);
            session.sendMessage(payload);
        } catch (Exception e) {
            log.error("Error sending server response to client: {}", session.getId(), e);
        }
    }
    // END

    /**
     * Sends a server message to all connected clients.
     * @param message The message content to send
     */
    private void sendServerMessageToClients(String message) {
        String json = messageConstructorService.generateMessageJson("server", null, MessageType.ServerInfo, message);
        WebSocketMessage<String> payload = new TextMessage(json);

        for (Session session : sessions.values()) {
            try {
                session.getSession().sendMessage(payload);
            } catch (Exception e) {
                log.error("Error sending message to session: {}", session.getSession().getId());
                log.error("", e);
            }
        }
    }

    /**
     * Broadcasts a message to all connected clients except the sender.
     * @param message The message to broadcast
     * @param senderId The session ID of the sender (excluded from broadcast)
     * @throws JsonProcessingException If message serialization fails
     */
    private void broadcastMessage(WebSocketMessage<?> message, String senderId) throws JsonProcessingException {
        String name = "anonymous";

        Optional<Session> sender = getSession(senderId);
        if (sender.isPresent())
            name = sender.get().getName();

        WebSocketMessage<String> payload = new TextMessage((String) message.getPayload());
        for (Session session : sessions.values()) {
            if (!session.getSession().getId().equals(senderId)) {
                try {
                    session.getSession().sendMessage(payload);
                } catch (Exception e) {
                    log.error("Error sending message to session: {}", session.getSession().getId());
                    log.error("", e);
                }
            }
        }
    }

    /**
     * Constructs a broadcast message with the given sender name and payload.
     * @param name The name of the message sender
     * @param payload The message content
     * @return WebSocket text message ready for transmission
     * @throws JsonProcessingException If message serialization fails
     */
    private WebSocketMessage<String> constructBroadcastMessage(String name, String payload) throws JsonProcessingException {
        String json = messageConstructorService.generateMessageJson(name, null, payload);
        return new TextMessage(json);
    }

    /**
     * Handles transport errors for WebSocket connections.
     * Logs error details but allows Spring to manage the session closure.
     * @param session The WebSocket session experiencing the error
     * @param exception The error that occurred
     * @throws Exception If an error occurs during error handling
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Websocket transport error for session {}: {}", session.getId(), exception.getMessage());
        log.error("Transport error details:", exception);
        // Don't close session here - let Spring handle it
    }

    /**
     * This method is called after a WebSocket connection has been closed.
     * It allows any necessary cleanup or processing related to the connection closure.
     *
     * @param session     the WebSocket session that has been closed
     * @param closeStatus the status object containing details on why the connection was closed
     * @throws Exception if an error occurs during the handling of the connection closure
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.info("Websocket closed: {}", session.getId());
        removeSession(session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * Retrieves a session by its ID.
     * @param id The session ID to look up
     * @return Optional containing the session if found, empty otherwise
     */
    private Optional<Session> getSession(String id) {
        return Optional.ofNullable(sessions.get(id));
    }

    /**
     * Adds a new session to the active sessions map.
     * @param session The session to add
     */
    private void addSession(Session session) {
        sessions.put(session.getSession().getId(), session);
    }

    /**
     * Removes a session from the active sessions map.
     * @param id The ID of the session to remove
     */
    private void removeSession(String id) {
        sessions.remove(id);
    }
}
