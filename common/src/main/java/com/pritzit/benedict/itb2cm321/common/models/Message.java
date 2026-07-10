package com.pritzit.benedict.itb2cm321.common.models;

import lombok.*;

/**
 * A message that can be sent between client and server.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private MessageHeader header;
    private MessageData data;
}