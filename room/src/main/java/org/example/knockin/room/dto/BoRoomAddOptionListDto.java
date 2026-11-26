package org.example.knockin.room.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class BoRoomAddOptionListDto {
    @Data
    public static class Request {
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        @Schema(description = "방 추가 옵션")
        private List<BoRoomAddOptionListDto.Response.RoomAddOptionItem> roomAddOptionItem;

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class RoomAddOptionItem {
            @Schema(description = "고유 식별 ID")
            private Long id;
            @Schema(description = "이름")
            private String name;
        }
    }
}
