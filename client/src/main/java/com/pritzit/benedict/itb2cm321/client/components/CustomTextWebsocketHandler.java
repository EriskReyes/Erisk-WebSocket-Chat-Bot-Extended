package com.pritzit.benedict.itb2cm321.client.components;

import com.pritzit.benedict.itb2cm321.client.gui.components.ChatPanel;
import com.pritzit.benedict.itb2cm321.common.models.Message;
import com.pritzit.benedict.itb2cm321.common.services.MessageConstructorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Custom WebSocket handler for the client application.
 * Handles incoming text messages, deserializes them, and notifies registered listeners.
 * Manages connection status and message consumption through callback mechanisms.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class CustomTextWebsocketHandler extends TextWebSocketHandler {
    private final MessageConstructorService messageConstructorService;

    private List<Consumer<Message>> messageConsumers = new ArrayList<>();
    private List<Consumer<Boolean>> connectionStatusListeners = new ArrayList<>();

    /**
     * Called after a WebSocket connection is successfully established.
     * Notifies all connection status listeners that connection is active.
     * @param session The established WebSocket session
     * @throws Exception If an error occurs during connection handling
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Websocket connected: {}", session.getId());
        notifyConnectionStatusListeners(true);
        super.afterConnectionEstablished(session);
    }

    /**
     * Handles incoming text messages from the server.
     * Deserializes the message and notifies all registered message consumers.
     * @param session The WebSocket session
     * @param message The received text message
     * @throws Exception If an error occurs during message processing
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.debug("Websocket message received: {}", message.getPayload());
        Message parsedMessage = messageConstructorService.deserializeMessage(message.getPayload(), session.getId());

        log.info("""
                        Message received:
                        Origin: {}
                        Destination: {}
                        Message Type: {}
                        Timestamp: {}
                        Message:
                        {}
                        """,
                parsedMessage.getHeader().getOrigin(),
                parsedMessage.getHeader().getDestination(),
                parsedMessage.getHeader().getType(),
                parsedMessage.getHeader().getTimestamp(),
                parsedMessage.getData().getMessage()
        );

        notifyMessageConsumers(parsedMessage);

        super.handleTextMessage(session, message);
    }

    /**
     * Notifies all registered message consumers about a new message.
     * @param message The message to distribute to consumers
     */
    private void notifyMessageConsumers(Message message) {
        messageConsumers.forEach(consumer -> consumer.accept(message));
    }

    /**
     * Called after a WebSocket connection is closed.
     * Notifies all connection status listeners that connection is inactive.
     * @param session The closed WebSocket session
     * @param status The status code indicating why the connection was closed
     * @throws Exception If an error occurs during closure handling
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("Websocket closed: {}", session.getId());
        notifyConnectionStatusListeners(false);
        super.afterConnectionClosed(session, status);
    }

    /**
     * Registers a consumer to receive incoming messages.
     * @param consumer The consumer callback to handle messages
     */
    public void addMessageConsumer(Consumer<Message> consumer){
        messageConsumers.add(consumer);
    }

    /**
     * Registers a listener to receive connection status updates.
     * @param listener The listener callback to handle connection status changes
     */
    public void addConnectionStatusListener(Consumer<Boolean> listener) {
        connectionStatusListeners.add(listener);
    }

    /**
     * Notifies all connection status listeners about connection state changes.
     * @param isConnected True if connected, false if disconnected
     */
    private void notifyConnectionStatusListeners(boolean isConnected) {
        connectionStatusListeners.forEach(listener -> listener.accept(isConnected));
    }
}
