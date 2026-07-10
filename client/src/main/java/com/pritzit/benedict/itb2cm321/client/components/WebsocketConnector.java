package com.pritzit.benedict.itb2cm321.client.components;

import com.pritzit.benedict.itb2cm321.client.config.WebsocketConfig;
import com.pritzit.benedict.itb2cm321.common.models.Message;
import com.pritzit.benedict.itb2cm321.common.services.MessageConstructorService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Component responsible for establishing and managing WebSocket client connections.
 * Handles automatic retry logic, connection status tracking, and message sending.
 * Attempts to reconnect up to MAX_RETRIES times with delays between attempts.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class WebsocketConnector {
    private final WebsocketConfig config;
    private final CustomTextWebsocketHandler handler;
    private final MessageConstructorService messageConstructorService;
    private WebSocketSession currentSession;
    private final List<Consumer<Boolean>> connectionStatusListeners = new ArrayList<>();
    private boolean connectionAttempted = false;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 2000; // 2 seconds

    /**
     * Initiates the WebSocket connection attempt.
     * Can only be called once; subsequent calls are ignored.
     * Sets up connection close listeners and begins the connection process.
     */
    public void startConnection() {
        if (connectionAttempted) {
            log.debug("Connection already attempted");
            return;
        }

        connectionAttempted = true;
        log.info("Connecting to websocket with name {}: {}", config.getName(), config.getUrl());

        // Listen for connection close events from the handler
        handler.addConnectionStatusListener(isConnected -> {
            if (!isConnected) {
                currentSession = null;
                notifyConnectionStatusListeners(false);
            }
        });

        attemptConnection();
    }

    /**
     * Attempts to establish a WebSocket connection.
     * On failure, automatically retries up to MAX_RETRIES times with delays.
     * Notifies connection status listeners of success or final failure.
     */
    private void attemptConnection() {
        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

        try {
            URI uri = new URI(config.getUrl() + "?name=" + config.getName());
            log.info("Connection attempt {} of {}", retryCount + 1, MAX_RETRIES);

            client.execute(handler, headers, uri).thenAccept((session) -> {
                currentSession = session;
                log.info("Websocket connection established successfully");
                retryCount = 0; // Reset retry count on success
                notifyConnectionStatusListeners(true);
            }).exceptionally(ex -> {
                log.error("Failed to connect to websocket (attempt {}): {}", retryCount + 1, ex.getMessage());

                if (retryCount < MAX_RETRIES - 1) {
                    retryCount++;
                    log.info("Retrying connection in {} ms...", RETRY_DELAY_MS);

                    // Schedule retry
                    new Thread(() -> {
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                            attemptConnection();
                        } catch (InterruptedException e) {
                            log.error("Retry interrupted", e);
                        }
                    }).start();
                } else {
                    log.error("Max retries reached. Connection failed.");
                    notifyConnectionStatusListeners(false);
                }
                return null;
            });
        } catch (URISyntaxException e) {
            log.error("Error connecting to websocket: {}", e.getMessage());
            notifyConnectionStatusListeners(false);
        }
    }

    /**
     * Sends a message to the server through the WebSocket connection.
     * If the connection is not open, logs an error and returns without sending.
     * @param message The message content to send
     */
    public void sendMessage(String message){
        if(currentSession == null || !currentSession.isOpen()){
            log.error("Websocket is not open");
            return;
        }

        String messageObject = messageConstructorService.generateMessageJson(config.getName(), null, message);
        try {
            currentSession.sendMessage(new TextMessage(messageObject));
        } catch (IOException e) {
            log.error("Could not send message to websocket: {}", e.getMessage());
        }
    }

    /**
     * Checks if the WebSocket connection is currently active.
     * @return True if connected and session is open, false otherwise
     */
    public boolean isConnected(){
        return currentSession != null && currentSession.isOpen();
    }

    /**
     * Registers a listener to receive connection status updates.
     * If already connected, immediately notifies the listener.
     * @param listener The listener callback to handle connection status changes
     */
    public void addConnectionStatusListener(Consumer<Boolean> listener) {
        connectionStatusListeners.add(listener);
        // If already connected, notify immediately
        if (isConnected()) {
            listener.accept(true);
        }
    }

    /**
     * Notifies all connection status listeners about connection state changes.
     * @param isConnected True if connected, false if disconnected
     */
    private void notifyConnectionStatusListeners(boolean isConnected) {
        connectionStatusListeners.forEach(listener -> listener.accept(isConnected));
    }
}
