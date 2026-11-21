package org.example.knockin.mate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.knockin.mate.entity.RoommateRequiredStatus;

import java.time.LocalDateTime;

@Data
public class RoommateRequestDto {
    @Data
    public static class Request {
        @Schema(description = "채팅 방 id")
        private Long chatRoomId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        @Schema(description = "신규 룸메이트 매칭 요청 정보")
        private RoommateMatchingRequiredInfo roommateMatchingRequiredInfo;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class RoommateMatchingRequiredInfo {
        @Schema(description = "룸메이트 요청 고유 식별 ID")
        private Long requiredId;

        @Schema(description = "요청자 멤버 고유 식별 ID")
        private Long requesterMemberId;

        @Schema(description = "피요청자 멤버 고유 식별 ID")
        private Long requesteeMemberId;

        @Schema(description = "상태")
        private RoommateRequiredStatus status;

        @Schema(description = "요청 시각")
        private LocalDateTime createdAt;

        @Schema(description = "수정 시각")
        private LocalDateTime updatedAt;
    }
}
