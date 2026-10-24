package org.example.knockin.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "R2 직접 업로드 방식의 게시글 등록 DTO")
public class BoardPreSignedDto {
    @Data
    @Schema(name = "BoardPreSignedSaveRequest")
    public static class Request {
        @NotBlank
        @Size(max = 50)
        @Schema(description = "게시글 제목")
        private String title;
        @NotBlank
        @Size(max = 500)
        @Schema(description = "게시글 내용")
        private String contents;
        @NotNull
        @Max(2000)
        @Schema(description = "보증금")
        private Integer deposit;
        @NotNull
        @Max(500)
        @Schema(description = "월세. 기존 게시글 등록 API의 필드명 유지")
        private Integer mountlyRent;
        @NotNull
        @Min(0)
        @Schema(description = "관리비")
        private Integer managementCost;
        @NotNull
        @Schema(description = "방 타입 고유 식별 ID")
        private Long roomTypeId;
        @NotNull
        @Schema(description = "지역 고유 식별 ID")
        private Long regionId;
        @Schema(description = "입주일 협의 가능 여부")
        private Boolean comeableDateNegotiable;
        @Schema(description = "입주 가능 일시", example = "2026-11-01T14:00:00")
        private LocalDateTime comeableDate;
        @Valid
        @Size(max = 10)
        @Schema(description = "R2 업로드를 완료한 이미지 목록. 생략, null 또는 빈 배열이면 이미지 없이 등록합니다. 최대 10장이며, 이미지가 있으면 썸네일은 1개여야 합니다.")
        private List<@NotNull FileDto> images;
        @Schema(description = "방 추가 옵션 고유 식별 ID 목록")
        private List<Long> extraOptionIds;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(name = "BoardPreSignedFileRequest")
        public static class FileDto {
            @NotBlank
            @Size(max = 300)
            @Schema(description = "POST /presigned-url 응답의 fileKey. R2 PUT 완료 후 전달")
            private String fileKey;
            @NotBlank
            @Size(max = 500)
            @Schema(description = "원본 파일명. POST /presigned-url 응답의 filename", example = "room.jpeg")
            private String originalFilename;
            @Schema(description = "썸네일 이미지 여부")
            private boolean thumbnail;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "BoardPreSignedSaveResponse")
    public static class Response {
        @Schema(description = "게시글 수정 일시", example = "2026-10-21T12:00:00")
        private LocalDateTime updatedAt;
    }
}
