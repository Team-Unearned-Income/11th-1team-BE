package org.example.knockin.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class BoardModifyDto {
    @Data
    public static class Request {

        @NotNull
        @Schema(description = "제목")
        private String title;

        @NotNull
        @Max(2000)
        @Schema(description = "보증금")
        private int deposit;

        @NotNull
        @Max(500)
        @Schema(description = "월세")
        private int monthlyRent;

        @NotNull
        @Schema(description = "관리비")
        private int managementCost;

        @NotNull
        @Schema(description = "룸 형태 ID")
        private Long roomTypeId;

        @NotNull
        @Schema(description = "지역 ID")
        private Long regionId;

        @Schema(description = "입주 협의 가능 여부")
        private Boolean comeableDateNegotiable;

        @Schema(description = "입주 가능시기")
        private LocalDateTime comeableDate;

        @Schema(description = "삭제 추가 옵션 ID 목록")
        private List<Long> deleteExtraOptionIds;

        @Schema(description = "신규 추가 옵션 ID 목록")
        private List<Long> newExtraOptionIds;

        @NotNull
        @Schema(description = "내용")
        private String contents;

        @Valid
        @Schema(description = "유지할 이미지 DTO")
        private List<ExistingFileDto> existingImages;

        @Valid
        @Schema(description = "신규 이미지 메타데이터 목록")
        private List<NewFileDto> newImages;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ExistingFileDto {
            @NotNull
            @Schema(description = "게시물 파일 식별 ID")
            private Long boardFileId;

            @Schema(description = "썸네일 여부")
            private boolean thumbnail;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class NewFileDto {
            @NotNull
            @Schema(description = "files 파트에서 매칭할 파일 인덱스")
            private Integer fileIndex;

            @Schema(description = "썸네일 여부")
            private boolean thumbnail;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        @Schema(description = "날짜 및 시간")
        private LocalDateTime updatedAt;
    }
}
