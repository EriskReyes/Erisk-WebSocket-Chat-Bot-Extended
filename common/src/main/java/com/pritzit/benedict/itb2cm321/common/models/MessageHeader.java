package com.pritzit.benedict.itb2cm321.common.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents the header section of a message exchanged between a client and server.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageHeader {
    private String origin;
    private String destination;
    private Instant timestamp;
    private MessageType type;
}