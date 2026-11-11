package org.example.knockin.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class ChatMessageDto {
    @Data
    public static class Request {
        @Schema(description = "FE 로컬 메시지 식별 ID")
        private String clientMessageId;

        @Schema(description = "채팅 메시지 콘텐츠 유형")
        private MessageType type;

        @Schema(description = "텍스트 메시지 본문")
        private String message;

        @Schema(description = "이미지 URL")
        private String imageUrl;
    }

    @Data
    @AllArgsConstructor
    public static class Response {
        @Schema(description = "FE 로컬 메시지 식별 ID")
        private String clientMessageId;

        @Schema(description = "발송자 회원 ID")
        private Long senderId;

        @Schema(description = "채팅 메시지 콘텐츠 유형")
        private MessageType type;

        @Schema(description = "텍스트 메시지 본문")
        private String contents;

        @Schema(description = "이미지 URL")
        private String imageUrl;

        public static Response chatMessage(ChatRoomMessageEvent event) {
            return new Response(
                    event.clientMessageId(),
                    event.senderId(),
                    event.messageType(),
                    event.message(),
                    event.imageUrl()
            );
        }

        public static Response userLeft(ChatRoomLeftEvent event) {
            return new Response(
                    null,
                    null,
                    MessageType.LEFT_ROOM,
                    event.message(),
                    null
            );
        }
    }
}
