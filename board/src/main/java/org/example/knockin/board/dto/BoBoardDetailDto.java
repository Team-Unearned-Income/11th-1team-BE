package org.example.knockin.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
public class BoBoardDetailDto {
    @Data
    public static class Request {
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        @Schema(description = "대표 이미지")
        private String thumbnailImage;
        @Schema(description = "제목")
        private String title;
        @Schema(description = "작성자")
        private String writer;
        @Schema(description = "작성자 고유 번호")
        private Long writerId;
        @Schema(description = "지역")
        private String region;
        @Schema(description = "보증금")
        private Long deposit;
        @Schema(description = "월세")
        private Long monthlyRent;
        @Schema(description = "즉시 입주 여부")
        private boolean comeableDateNegotiable;
        @Schema(description = "입주가능일")
        private LocalDateTime comeableDate;
        @Schema(description = "등록일")
        private LocalDateTime createdAt;
        @Schema(description = "조회수")
        private Long hits;
        @Schema(description = "삭제 여부")
        private boolean isDeleted;
    }
}