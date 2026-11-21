package org.example.knockin.mate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class CalendarDto {
    @Data
    public static class Request {
        @Valid
        @Schema(description = "캘린더 정보")
        private CalendarInfoDto calendar;

        @NotBlank
        @Size(min = 1, max = 50)
        @Schema(description = "카테고리명")
        private String categoryName;

        @NotEmpty
        @Schema(description = "담당자 목록")
        private List<Long> memberIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        @Schema(description = "수정 일시")
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalendarInfoDto {
        @NotNull
        @Schema(description = "내 룸메 고유 식별 ID")
        private Long myRoommateId;

        @NotBlank
        @Size(min = 1, max = 50)
        @Schema(description = "일정 제목")
        private String title;

        @NotBlank
        @Size(min = 1, max = 500)
        @Schema(description = "일정 내용")
        private String contents;

        @NotNull
        @Schema(description = "일정 시작일")
        private LocalDateTime startDate;

        @NotNull
        @Schema(description = "일정 종료일 (당일치기면 일정 시작일과 동일)")
        private LocalDateTime endDate;
    }
}