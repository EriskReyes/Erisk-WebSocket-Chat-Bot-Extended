package com.pritzit.benedict.itb2cm321.common.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pritzit.benedict.itb2cm321.common.models.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageConstructorService {

    private final ObjectMapper mapper;
    /**
     * Generates a message with the given origin, destination and payload.
     * @param origin The origin of the message. This is the name of the client that sent the message.
     * @param destination The destination of the message. This is the name of the client that the message is intended for. Null if it is a broadcast message.
     * @param type The type of the message. This is used to determine the format of the message payload.
     * @param payload The payload of the message. This is the actual message content. The format of the message content is defined by the client.
     * @return A Message object containing the origin, destination and payload of the message.
     */
    public Message generateMessage(String origin, String destination, MessageType type, String payload){
        MessageData data = MessageData.builder()
                .message(payload)
                .build();

        MessageHeader header = MessageHeader.builder()
                .origin(origin)
                .destination(destination)
                .type(type)
                .timestamp(Instant.now())
                .build();

        Message message = Message.builder()
                .header(header)
                .data(data)
                .build();

        log.debug("Generated message: {}", message);

        return message;
    }

    /**
     * Converts the given {@link Message} object into its JSON string representation
     * using the configured {@link ObjectMapper}.
     *
     * @param message The {@link Message} object to be serialized into JSON.
     * @return A JSON string representation of the {@link Message} object, or null
     *         if an error occurs during the serialization process.
     */
    public String generateMessageJson(Message message){
        try {
            return mapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.error("Error processing message: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Serializes a SessionInfo object into its JSON string representation.
     * @param sessionInfo The SessionInfo object to serialize
     * @return JSON string representation of the SessionInfo, or null if serialization fails
     */
    public String serializeSessionInfo(SessionInfo sessionInfo){
        try {
            return mapper.writeValueAsString(sessionInfo);
        } catch (JsonProcessingException e) {
            log.error("Error processing session info: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Converts a message generated with the specified origin, destination, and payload
     * into its JSON representation using the provided ObjectMapper.
     *
     * @param origin The origin of the message. This is the name of the client that sent the message.
     * @param destination The destination of the message. This is the name of the client that the message is intended for. Null if it is a broadcast message.
     * @param payload The payload of the message. This is the actual message content. The format of the message content is defined by the client.
     * @return A JSON string representation of the generated message, or null if an error occurs during processing.
     */
    public String generateMessageJson(String origin, String destination, String payload){
        return generateMessageJson(origin, destination, MessageType.Chat, payload);
    }

    /**
     * Generates a message and converts it to JSON in a single operation.
     * @param origin The origin of the message
     * @param destination The destination of the message (null for broadcast)
     * @param type The type of the message
     * @param payload The message content
     * @return JSON string representation of the generated message
     */
    public String generateMessageJson(String origin, String destination, MessageType type, String payload){
        Message message = generateMessage(origin, destination, type, payload);
        return generateMessageJson(message);
    }

    /**
     * Deserializes a JSON string into a SessionInfo object.
     * Returns a default SessionInfo with 0 connected clients if deserialization fails.
     * @param json The JSON string to deserialize
     * @return SessionInfo object, or default SessionInfo if parsing fails
     */
    public SessionInfo deserializeSessionInfo(String json){
        try {
            return mapper.readValue(json, SessionInfo.class);
        } catch (JsonProcessingException e) {
            log.warn("Error processing session info: {} - returning default SessionInfo", e.getMessage());
            return SessionInfo.builder()
                    .connectedClients(0)
                    .build();
        }
    }

    /**
     * Deserializes a JSON string into a Message object.
     * Returns a default Message containing the raw JSON if deserialization fails.
     * @param json The JSON string to deserialize
     * @param sender The sender ID to use in case of parsing failure
     * @return Message object, or default Message with raw JSON if parsing fails
     */
    public Message deserializeMessage(String json, String sender){
        try {
            return mapper.readValue(json, Message.class);
        } catch (JsonProcessingException e) {
            log.warn("Error processing message: {} - returning default Message", e.getMessage());
            return Message.builder()
                    .header(MessageHeader.builder().origin(sender == null ? "unknown" : sender).build())
                    .data(MessageData.builder().message(json).build())
                    .build();
        }
    }
}
