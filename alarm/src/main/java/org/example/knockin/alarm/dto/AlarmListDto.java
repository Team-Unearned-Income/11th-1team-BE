package org.example.knockin.alarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AlarmListDto {
    @Data
    public static class Request {
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        @Schema(description = "알림 목록")
        private List<Alarm> alarms;

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Alarm {
            @Schema(description = "알람 고유 번호")
            private Long id;
            @Schema(description = "제목")
            private String title;
            @Schema(description = "내용")
            private String contents;
            @Schema(description = "읽음 여부")
            private Boolean isRead;
            @Schema(description = "알람 만료 시간")
            private LocalDateTime expiredAt;
            @Schema(description = "날짜 및 시간")
            private LocalDateTime createAt;
        }
    }
}
