package org.example.knockin.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.knockin.mate.dto.RoommateRequestDto.Response;

@Getter
@AllArgsConstructor
@Schema(description = "채팅방 WebSocket 공통 응답 envelope")
public class ChatSocketResponse<T> {
    @Schema(description = "WebSocket 이벤트 유형")
    private EventType eventType;

    @Schema(description = "채팅방 ID")
    private Long chatRoomId;

    @Schema(
            description = "이벤트별 payload",
            oneOf = {ChatMessageDto.Response.class, Response.class}
    )
    private T payload;

    @Schema(description = "서버 broadcast 시각")
    private LocalDateTime createdAt;

    public static <T> ChatSocketResponse<T> of(EventType eventType, Long chatRoomId, T payload) {
        return new ChatSocketResponse<>(eventType, chatRoomId, payload, LocalDateTime.now());
    }

    public static <T> ChatSocketResponse<T> of(EventType eventType, Long chatRoomId, T payload, LocalDateTime createdAt) {
        return new ChatSocketResponse<>(eventType, chatRoomId, payload, createdAt);
    }
}
