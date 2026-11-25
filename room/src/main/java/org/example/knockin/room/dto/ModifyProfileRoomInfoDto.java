package org.example.knockin.room.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.knockin.room.entity.RoomProfileType;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ModifyProfileRoomInfoDto {
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Request {
        @Schema(description = "유형")
        @NotNull
        private RoomProfileType type;
        @Schema(description = "min deposit")
        @PositiveOrZero
        private Integer minDeposit;
        @Schema(description = "max deposit")
        @PositiveOrZero
        private Integer maxDeposit;
        @Schema(description = "min mounth rent")
        @JsonProperty("minMonthlyRent")
        @JsonAlias("minMounthRent")
        @PositiveOrZero
        private Integer minMounthRent;
        @Schema(description = "max mounth rent")
        @JsonProperty("maxMonthlyRent")
        @JsonAlias("maxMounthRent")
        @PositiveOrZero
        private Integer maxMounthRent;
        @Schema(description = "come enable at")
        @NotNull
        private LocalDateTime comeEnableAt;
        @Schema(description = "region")
        @NotEmpty
        private List<Long> region;
        @Schema(description = "방 프로필")
        @NotEmpty
        private List<Long> roomProfile;
        @Schema(description = "deposit")
        @PositiveOrZero
        private Integer deposit;
        @Schema(description = "mounth rent")
        @JsonProperty("monthlyRent")
        @JsonAlias("mounthRent")
        @PositiveOrZero
        private Integer mounthRent;
        @Schema(description = "is comeable at negotiable")
        private boolean isComeableAtNegotiable;
    }

    @Data
    @Builder
    public static class Response {
        @Schema(description = "수정 일시")
        private LocalDateTime updatedAt;
    }
}