package com.pritzit.benedict.itb2cm321.server.components;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * WebSocket middleware implementing a three-way handshake protocol (SYN-SYN/ACK-ACK).
 * Ensures secure connection establishment before allowing normal message flow.
 * Automatically closes connections that fail to complete handshake within timeout period.
 */
@Slf4j
public class SynAckMiddleware extends WebSocketHandlerDecorator {

    private static final String ATTR_STATE = "hs.state";
    private static final String ATTR_TOKEN = "hs.token";
    private static final Duration HANDSHAKE_TIMEOUT = Duration.ofSeconds(10);
    private final ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);

    private enum State { NEW, SYN_RCVD, ESTABLISHED }

    public SynAckMiddleware(WebSocketHandler delegate) {
        super(delegate);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.debug("WS connected: {}", session.getId());
        session.getAttributes().put(ATTR_STATE, State.NEW);
        log.debug("Starting Handshake");
        ses.schedule(() -> closeIfNotEstablished(session), HANDSHAKE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        log.debug("WS connected, awaiting SYN from client: {}", session.getId());
    }

    /**
     * Closes the session if handshake was not completed within the timeout period.
     * @param session The WebSocket session to check and potentially close
     */
    private void closeIfNotEstablished(WebSocketSession session) {
        try {
            State s = (State) session.getAttributes().getOrDefault(ATTR_STATE, State.NEW);
            if (s != State.ESTABLISHED && session.isOpen()) {
                log.warn("Handshake timeout; closing session {}", session.getId());
                session.close(CloseStatus.POLICY_VIOLATION);
            }
        } catch (IOException ignored) {}
    }

    /**
     * Handles incoming messages during the handshake process.
     * Processes SYN, ACK messages and validates the handshake flow.
     * Once established, forwards messages to the underlying handler.
     * @param session The WebSocket session
     * @param message The incoming message
     * @throws Exception If an error occurs during message handling
     */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        Map<String, Object> attrs = session.getAttributes();
        State state = (State) attrs.getOrDefault(ATTR_STATE, State.NEW);

        if (!(message instanceof TextMessage)) {
            session.close(new CloseStatus(4003, "Text frames only during handshake"));
            return;
        }

        String payload = ((TextMessage) message).getPayload().trim();

        switch (state) {
            case NEW:
                if (!payload.equalsIgnoreCase("SYN")) {
                    session.close(new CloseStatus(4000, "Expected SYN"));
                    return;
                }
                String token = UUID.randomUUID().toString();
                attrs.put(ATTR_TOKEN, token);
                attrs.put(ATTR_STATE, State.SYN_RCVD);
                session.sendMessage(new TextMessage("SYN-ACK " + token));
                return;

            case SYN_RCVD:
                if (!payload.startsWith("ACK ")) { // note the space to avoid off-by-one bugs
                    session.close(new CloseStatus(4001, "Expected ACK <token>"));
                    return;
                }
                String recv = payload.substring(4).trim();
                String expected = (String) attrs.get(ATTR_TOKEN);
                if (!expected.equals(recv)) {
                    session.close(new CloseStatus(4002, "ACK token mismatch"));
                    return;
                }
                attrs.put(ATTR_STATE, State.ESTABLISHED);
                session.sendMessage(new TextMessage("ESTABLISHED"));
                log.info("Handshake established for session {}", session.getId());
                // Now that we’re established, optionally notify the downstream handler:
                super.afterConnectionEstablished(session);
                return;

            case ESTABLISHED:
                // Handshake done → forward to your real handler
                super.handleMessage(session, message);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
    }
}