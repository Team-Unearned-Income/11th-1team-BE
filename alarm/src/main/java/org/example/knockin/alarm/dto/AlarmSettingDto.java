package org.example.knockin.alarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
public class AlarmSettingDto {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @Schema(description = "설정 ID")
        private Long settingId;
        @Schema(description = "활성화 여부")
        private Boolean enabled;
    }

    @Data
    @Builder
    public static class Response {
        @Schema(description = "수정 일시")
        private LocalDateTime updatedAt;
    }
}