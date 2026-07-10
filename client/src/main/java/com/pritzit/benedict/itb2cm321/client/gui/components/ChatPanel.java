package com.pritzit.benedict.itb2cm321.client.gui.components;

import com.pritzit.benedict.itb2cm321.client.components.CustomTextWebsocketHandler;
import com.pritzit.benedict.itb2cm321.client.components.WebsocketConnector;
import com.pritzit.benedict.itb2cm321.common.models.Message;
import com.pritzit.benedict.itb2cm321.common.models.MessageType;
import com.pritzit.benedict.itb2cm321.common.models.SessionInfo;
import com.pritzit.benedict.itb2cm321.common.services.MessageConstructorService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;

@Slf4j
@RequiredArgsConstructor
@Component
public class ChatPanel extends JPanel {
    private final MessageConstructorService messageConstructorService;
    @Value("${spring.application.name}")
    private String clientName;
    private final JTextArea chatArea = new JTextArea();
    private final JTextField chatInput = new JTextField();
    private final JButton sendButton = new JButton("Send");
    private final WebsocketConnector websocketConnector;
    private final CustomTextWebsocketHandler handler;
    private JLabel activeSessionsLabel;

    @PostConstruct
    public void init(){
        log.info("ChatPanel init() called");
        this.setLayout(new BorderLayout());

        chatArea.setEditable(false);
        this.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.add(new JLabel("Client: " + clientName), BorderLayout.WEST);

        activeSessionsLabel = new JLabel("Active Sessions: " + 0);
        infoPanel.add(activeSessionsLabel, BorderLayout.EAST);

        this.add(infoPanel, BorderLayout.NORTH);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(chatInput);
        panel.add(sendButton);

        // FORCE enable fields - ALWAYS enabled regardless of connection
        log.info("Enabling input fields...");
        sendButton.setEnabled(true);
        chatInput.setEnabled(true);
        chatInput.setEditable(true);
        log.info("Input enabled: {}, Button enabled: {}", chatInput.isEnabled(), sendButton.isEnabled());

        sendButton.addActionListener(e -> sendMessage());
        chatInput.addActionListener(e -> sendMessage()); // Send on Enter key

        this.add(panel, BorderLayout.SOUTH);

        // Listen for connection status changes but DON'T disable fields
        websocketConnector.addConnectionStatusListener(isConnected ->
            SwingUtilities.invokeLater(() -> {
                if (isConnected) {
                    appendMessage("*** Connected to server ***");
                    chatInput.requestFocus();
                } else {
                    appendMessage("*** Disconnected from server - Retrying... ***");
                }
                // ALWAYS keep fields enabled
                sendButton.setEnabled(true);
                chatInput.setEnabled(true);
            })
        );

        handler.addMessageConsumer((message) -> SwingUtilities.invokeLater(() -> {
            switch (message.getHeader().getType()){
                case Chat:
                    appendMessage(message.getHeader().getOrigin() +": " + message.getData().getMessage());
                    break;
                case ServerInfo:
                    updateServerInfo(message);
                    break;
                default:
                    log.warn("Unspecified message type received");
            }
        }));

        // Start connection after all listeners are registered
        websocketConnector.startConnection();
    }

    private void updateServerInfo(Message message){
        String messageContent = message.getData().getMessage();

        // Check if it's a JSON object (periodic server updates) or plain text (command response)
        if(messageContent != null && messageContent.trim().startsWith("{")){
            // Try to deserialize as SessionInfo (periodic server updates)
            SessionInfo sessionInfo = messageConstructorService.deserializeSessionInfo(messageContent);

            // If it's a valid SessionInfo object, update the sessions label
            if(sessionInfo != null && sessionInfo.getConnectedClients() > 0){
                log.info("Server info received: {}", sessionInfo);
                SwingUtilities.invokeLater(() -> {
                    activeSessionsLabel.setText("Active Sessions: " + sessionInfo.getConnectedClients());
                });
            }
        } else {
            // It's a command response (like @server math or @server info result)
            // Display it in the chat area
            appendMessage("server: " + messageContent);
        }
    }

    /**
     * Sends a chat message from the client to the server through the connected websocket.
     *
     * This method retrieves the message text entered by the user in the input field. If the
     * input field is empty, the method exits without performing any actions. Otherwise, the
     * method clears the input field, appends the sent message to the chat display area with the
     * client name as the prefix, and sends the message to the server using the websocket connector.
     *
     * Preconditions:
     * - The websocket connection should be established prior to sending messages.
     *
     * Postconditions:
     * - If a non-empty message is provided, it is appended to the chat area for local display
     *   and sent through the websocket connection to the server.
     *
     * Throws:
     * - No exceptions are thrown explicitly by this method. Errors related to websocket message
     *   transmission are logged but do not affect the execution of this method.
     */
    public void sendMessage(){
        String message = chatInput.getText().trim();
        if(message.isEmpty()){
            return;
        }

        chatInput.setText("");

        // Don't show @server commands in chat, only regular messages
        if(!message.startsWith("@server ")){
            appendMessage(clientName + ": " + message);
        } else {
            // Show feedback that command was sent
            appendMessage("→  Befehl an den Server gesendet...");
        }

        websocketConnector.sendMessage(message);
    }

    /**
     * Appends a message to the chat display area.
     *
     * This method takes the provided message string and appends it to the chat area, followed by a new line.
     * It is typically used to update the chat display with incoming or outgoing messages.
     *
     * @param message The message string to be appended to the chat area. It can represent messages
     *                received from the server or sent by the client.
     */
    public void appendMessage(String message){
        chatArea.append(message + "\n");
    }
}
