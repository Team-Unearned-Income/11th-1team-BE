package org.example.knockin.room.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class BoRoomTypeDetailDto {
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
        @Schema(description = "이름")
        private String name;
        @Schema(description = "이미지")
        private String image;
    }
}