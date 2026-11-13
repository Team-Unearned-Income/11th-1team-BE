package org.example.knockin.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.example.knockin.verification.entity.AuthenticationType;
import org.example.knockin.board.entity.RoommateBoardBadgeType;
import org.example.knockin.member.entity.Gender;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MyBoardListDto {
    @Data
    public static class Request {
    }

    @Data
    @Builder
    public static class Response {
        private List<BoardItem> boards;

        @Data
        @Builder
        public static class BoardItem {
            @Schema(description = "게시물 ID")
            private Long id;
            @Schema(description = "대표 이미지 URL")
            private String imageUrl;
            @Schema(description = "게시물 제목")
            private String title;
            @Schema(description = "보증금")
            private Integer deposit;
            @Schema(description = "월세")
            private Integer monthlyRent;
            @Schema(description = "관리비")
            private Integer managementCost;
            @Schema(description = "룸 형태")
            private List<String> roomTypes;
            @Schema(description = "입주가능시기")
            private LocalDateTime comeableDate;
            @Schema(description = "위치")
            private String regionFullName;
            @Schema(description = "작성자 고유 식별 ID")
            private Long memberId;
            @Schema(description = "작성자 이름")
            private String memberName;
            @Schema(description = "작성자 프로필 사진 URL")
            private String memberProfileImageUrl;
            @Schema(description = "작성자 나이")
            private Integer memberAge;
            @Schema(description = "작성자 성별")
            private Gender gender;
            @Schema(description = "신원 인증")
            private List<AuthenticationType> authentications;
            @Schema(description = "조회수")
            private Long hits;
            @Schema(description = "게시글 태그 뱃지 목록")
            private List<RoommateBoardBadgeType> badges;
            @Schema(description = "관심 여부")
            private boolean interested;
            @Schema(description = "생성일자")
            private LocalDateTime createdAt;
        }
    }
}
