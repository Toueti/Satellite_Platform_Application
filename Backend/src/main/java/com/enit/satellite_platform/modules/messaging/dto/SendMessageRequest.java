package com.enit.satellite_platform.modules.messaging.dto;

import com.enit.satellite_platform.modules.messaging.entities.MessageType;

@lombok.Data
public class SendMessageRequest {
    private String recipientId;
    private String content;
    private MessageType messageType;
}
