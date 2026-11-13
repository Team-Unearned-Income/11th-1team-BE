package org.example.knockin.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BoBoardListDto {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @Schema(description = "제목 작성자 지역 검색어")
        private String searchKeyword;
        @Schema(description = "상형")
        private Boolean isDeleted;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        @Schema(description = "게시글 목록")
        private List<BoardInfo> boardInfoList;

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class BoardInfo {
            @Schema(description = "수정 일시")
            private Long id;
            @Schema(description = "제목")
            private String title;
            @Schema(description = "작성자")
            private String writer;
            @Schema(description = "지역")
            private String region;
            @Schema(description = "입주가능일")
            private LocalDateTime comeableDate;
            @Schema(description = "삭제 여부")
            private boolean isDeleted;
            @Schema(description = "등록일")
            private LocalDateTime createdAt;
            @Schema(description = "즉시 입주 여부")
            private boolean comeableDateNegotiable;
        }
    }
}