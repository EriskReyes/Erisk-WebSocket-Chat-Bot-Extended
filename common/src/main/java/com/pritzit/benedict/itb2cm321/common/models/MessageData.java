package com.pritzit.benedict.itb2cm321.common.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the data section of a message exchanged between a client and server.
 * This class can be used to encapsulate additional information specific to a message.
 * It is a part of the messaging system alongside related classes such as Message and MessageHeader.
 * The exact structure and fields of the data should be defined as per use-case requirements.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageData {
    private String message;
}