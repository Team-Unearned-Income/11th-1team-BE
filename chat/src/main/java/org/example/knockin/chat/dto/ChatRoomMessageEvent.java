package org.example.knockin.chat.dto;

import lombok.Builder;

@Builder
public record ChatRoomMessageEvent(
        Long chatRoomId,
        Long senderId,
        String clientMessageId,
        MessageType messageType,
        String message,
        String imageUrl
) {
}
