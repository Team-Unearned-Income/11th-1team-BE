package org.example.knockin.alarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.knockin.alarm.entity.AlarmType;

import java.time.LocalDateTime;

@Data
public class AlarmSendDto {

    @Data
    public static class Request {
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        @Schema(description = "고유 식별 ID")
        private Long id;
        @Schema(description = "제목")
        private String title;
        @Schema(description = "내용")
        private String contents;
        @Schema(description = "읽음 여부")
        private boolean isRead;
        @Schema(description = "만료 일시")
        private LocalDateTime expiredAt;
        @Schema(description = "생성 일시")
        private LocalDateTime createdAt;
        @Schema(description = "event")
        private Event event;

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Event {
            @Schema(description = "event id")
            private Long eventId;
            @Schema(description = "알림 유형")
            private AlarmType alarmType;
        }
    }
}