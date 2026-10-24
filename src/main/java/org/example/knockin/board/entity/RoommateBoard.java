package org.example.knockin.board.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.knockin.board.dto.BoardModifyDto;
import org.example.knockin.board.dto.BoardPreSignedModifyDto;
import org.example.knockin.member.entity.Member;
import org.example.knockin.meta.entity.Region;
import org.example.knockin.room.entity.RoomType;
import org.example.knockin.global.entity.BaseEntity;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "roommate_board")
public class RoommateBoard extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE) private Member member;

    @Column(name = "title", length = 50)
    private String title;

    @Column(name = "contents", length = 500)
    private String contents;

    @Column(name = "deposit")
    private Integer deposit;

    @Column(name = "monthly_rent")
    private Integer monthlyRent;

    @Column(name = "management_cost")
    private Integer managementCost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(name ="comeable_date_negotiable")
    private Boolean comeableDateNegotiable;

    @Column(name = "comeable_date")
    private LocalDateTime comeableDate;

    @ColumnDefault("false")
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @ColumnDefault("0")
    @Column(name = "hits", nullable = false)
    @Builder.Default
    private Long hits = 0L;

    @Column(length = 500)
    private String rejectReason;

    public void modifyBasicInfo(BoardModifyDto.Request request) {
        title = request.getTitle();
        contents = request.getContents();
        deposit = request.getDeposit();
        monthlyRent = request.getMonthlyRent();
        managementCost = request.getManagementCost();
        comeableDateNegotiable = request.getComeableDateNegotiable();
        comeableDate = request.getComeableDate();
    }

    public void modifyBasicInfo(BoardPreSignedModifyDto.Request request) {
        title = request.getTitle();
        contents = request.getContents();
        deposit = request.getDeposit();
        monthlyRent = request.getMonthlyRent();
        managementCost = request.getManagementCost();
        comeableDateNegotiable = request.getComeableDateNegotiable();
        comeableDate = request.getComeableDate();
    }

    public void modifyRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public void modifyRegion(Region region) {
        this.region = region;
    }

    public void softDelete() {
        this.isDeleted = true;
    }

    public void softDelete(String rejectReason) {
        this.isDeleted = true;
        this.rejectReason = rejectReason;
    }

    public void recoverDelete() {
        this.isDeleted = false;
    }
}
