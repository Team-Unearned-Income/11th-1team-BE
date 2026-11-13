package org.example.knockin.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.knockin.util.dto.Compatibility;
import org.example.knockin.verification.entity.AuthenticationType;
import org.example.knockin.board.entity.RoommateBoardBadgeType;
import org.example.knockin.life.entity.LifePatternType;
import java.time.LocalDateTime;
import java.util.List;
import org.example.knockin.member.entity.Gender;

@Data
@NoArgsConstructor
public class BoardDetailDto {

    @Data
    public static class Request {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        @Schema(description = "고유 식별 ID")
        private Long boardId;

        @Schema(description = "이미지 정보 목록")
        private List<FileDetailDto> images;

        @Schema(description = "제목")
        private String title;

        @Schema(description = "보증금")
        private Integer deposit;

        @Schema(description = "관리비")
        private Integer managementCost;

        @Schema(description = "월세")
        private Integer monthlyRent;

        @Schema(description = "방 타입명")
        private String roomTypeName;

        @Schema(description = "지역명 풀네임")
        private String regionFullName;

        @Schema(description = "입주 협의 가능 여부")
        private Boolean comeableDateNegotiable;

        @Schema(description = "입주 가능일")
        private LocalDateTime comeableDate;

        @Schema(description = "날짜 및 시간")
        private LocalDateTime createdAt;

        @Schema(description = "조회수")
        private Long hits;

        @Schema(description = "내용")
        private String contents;

        @Schema(description = "방 추가 옵션 목록")
        private List<RoomExtraOptionInfo> roomExtraOptions;

        @Schema(description = "생활패턴")
        private List<Lifestyle> lifeStyles;

        @Schema(description = "선호 룸메이트 조건 목록")
        private List<Condition> conditions;

        @Schema(description = "선호 룸메이트 중요 조건 목록")
        private List<ConditionWeight> conditionWeights;

        @Schema(description = "등록자 고유 식별 ID")
        private Long memberId;

        @Schema(description = "등록자 이름")
        private String memberName;

        @Schema(description = "등록자 프로필 사진 URL")
        private String memberProfileImageUrl;

        @Schema(description = "등록자 나이")
        private Integer memberAge;

        @Schema(description = "등록자 성별")
        private Gender gender;

        @Schema(description = "승인된 신원 인증")
        private List<AuthenticationType> authentications;

        @Schema(description = "적합도")
        private Compatibility compatibility;

        @Schema(description = "관심 여부")
        private boolean interested;

        @Schema(description = "본인 여부")
        private boolean mine;

        @Schema(description = "게시글 태그 뱃지 목록")
        private List<RoommateBoardBadgeType> badges;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class FileDetailDto {
            @Schema(description = "게시물 파일 식별 ID")
            private Long boardFileId;
            @Schema(description = "이미지 URL")
            private String url;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class RoomExtraOptionInfo {
            @Schema(description = "고유 식별 ID")
            private Long extraOptionId;

            @Schema(description = "옵션명")
            private String name;

            @Schema(description = "이미지 URL")
            private String imageUrl;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Lifestyle {
            @Schema(description = "고유 식별 ID")
            private Long lifestyleId;
            @Schema(description = "이름")
            private String name;
            @Schema(description = "값")
            private String value;
            @Schema(description = "설명")
            private String description;
            @Schema(description = "타입/유형")
            private LifePatternType type;
            @Schema(description = "이미지 URL")
            private String imageUrl;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Condition {
            @Schema(description = "고유 식별 ID")
            private Long conditionId;
            @Schema(description = "이름")
            private String name;
            @Schema(description = "값")
            private String value;
            @Schema(description = "설명")
            private String description;
            @Schema(description = "타입/유형")
            private LifePatternType type;
            @Schema(description = "이미지 URL")
            private String imageUrl;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ConditionWeight {
            @Schema(description = "고유 식별 ID")
            private Long weightConditionId;
            @Schema(description = "이름")
            private String name;
            @Schema(description = "이미지 URL")
            private String imageUrl;
        }

    }
}
