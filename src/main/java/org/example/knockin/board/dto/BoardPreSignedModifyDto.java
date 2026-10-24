package org.example.knockin.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.knockin.board.dto.BoardModifyDto.Request.ExistingFileDto;

@Schema(description = "R2 직접 업로드 방식의 게시글 수정 DTO")
public class BoardPreSignedModifyDto {
    @Data
    @Schema(name = "BoardPreSignedModifyRequest")
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
        @Schema(description = "월세. 기존 게시글 수정 API의 필드명 유지")
        private Integer monthlyRent;
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
        @Schema(description = "삭제할 방 추가 옵션 ID 목록")
        private List<Long> deleteExtraOptionIds;
        @Schema(description = "새로 추가할 방 추가 옵션 ID 목록")
        private List<Long> newExtraOptionIds;
        @Valid
        @Size(max = 10)
        @Schema(description = "유지할 기존 이미지와 수정 후 썸네일 여부. boardFileId는 수정할 게시글에 속해야 합니다. 목록에서 빠진 이미지는 삭제되며 생략, null, 빈 배열은 기존 이미지 전체 삭제를 뜻합니다.")
        private List<ExistingFileDto> existingImages;
        @Valid
        @Size(max = 10)
        @Schema(description = "R2 PUT 완료 후 추가할 이미지. 저장 API와 동일한 fileKey, originalFilename, thumbnail을 전달합니다. 생략, null, 빈 배열이면 추가하지 않습니다. 기존 이미지와 합쳐 최대 10장, 이미지가 있으면 썸네일은 정확히 1장이어야 합니다.")
        private List<FileDto> newImages;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
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
    public static class Response {
        @Schema(description = "게시글 수정 일시", example = "2026-10-21T12:00:00")
        private LocalDateTime updatedAt;
    }
}
