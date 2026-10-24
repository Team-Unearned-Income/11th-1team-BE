package org.example.knockin.meta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class PresignedUrlDto {
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Request {
        @Schema(description = "업로드할 파일 정보 목록")
        @Valid
        @NotEmpty
        @Size(max = 10)
        private List<FileInfo> fileInfoList;

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class FileInfo {
            @Schema(description = "원본 파일명")
            @NotBlank
            @Size(max = 500)
            private String originalFilename;
            @Schema(description = "파일의 MIME 타입")
            @NotBlank
            @Pattern(regexp = "image/(jpeg|png|webp|gif)")
            private String contentType;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        @Schema(description = "파일별 업로드 및 접근 정보 목록")
        private List<FileUploadInfo> fileUploadInfoList;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class FileUploadInfo {
            @Schema(description = "원본 파일명")
            private String filename;
            @Schema(description = "R2 임시 업로드 주소")
            private String uploadUrl;
            @Schema(description = "퍼블릭 URL")
            private String fileUrl;
            @Schema(description = "고유 파일 경로")
            private String fileKey;
        }
    }
}
