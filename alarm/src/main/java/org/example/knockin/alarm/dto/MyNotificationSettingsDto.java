package org.example.knockin.alarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
public class MyNotificationSettingsDto {
    @Data
    public static class Request {
    }

    @Data
    @Builder
    public static class Response {
        @Schema(description = "alarms settings")
        private List<AlarmSettingItem> alarmsSettings;

        @Data
        @Builder
        public static class AlarmSettingItem {
            @Schema(description = "고유 식별 ID")
            private Long id;
            @Schema(description = "이름")
            private String name;
            @Schema(description = "활성화 여부")
            private Boolean isEnable;
        }
    }
}