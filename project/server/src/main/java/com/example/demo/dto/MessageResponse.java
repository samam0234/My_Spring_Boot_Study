package com.example.demo.dto;

import com.example.demo.domain.Message;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        String senderUsername,
        String receiverUsername,
        String content,
        LocalDateTime createdAt
) {

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getSender().getUsername(),
                message.getReceiver().getUsername(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
