package org.example.knockin.chat.dto;


import java.time.LocalDateTime;

public record ChatRoomLeftEvent(
        Long chatRoomId,
        LocalDateTime leftAt,
        String message
) {
}