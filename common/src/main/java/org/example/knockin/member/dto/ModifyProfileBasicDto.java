package org.example.knockin.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.knockin.member.entity.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ModifyProfileBasicDto {
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Request {
        @Schema(description = "이름")
        @NotBlank
        @Size(max = 10)
        private String name;
        @Schema(description = "birth")
        @NotNull
        @Past
        private LocalDate birth;
        @Schema(description = "gender")
        @NotNull
        private Gender gender;
        @Schema(description = "이메일")
        @Email
        @Size(max = 50)
        private String email;
        @Schema(description = "약관")
        @NotEmpty
        private List<Long> terms;

        public static Request of(SaveProfileBasicDto.Request request) {
            return Request.builder()
                    .name(request.getName())
                    .birth(request.getBirth())
                    .gender(request.getGender())
                    .email(request.getEmail())
                    .terms(request.getTerms())
                    .build();
        }
    }

    @Data
    @Builder
    public static class Response {
        @Schema(description = "수정 일시")
        private LocalDateTime updatedAt;
    }
}