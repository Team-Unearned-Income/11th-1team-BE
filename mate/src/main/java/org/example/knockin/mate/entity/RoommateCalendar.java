package org.example.knockin.mate.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.knockin.global.entity.BaseEntity;
import org.example.knockin.mate.dto.CalendarDto;
import org.example.knockin.member.entity.Member;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "roommate_calendar")
public class RoommateCalendar extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "my_roommate_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private MyRoommate myRoommate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, comment = "작성자")
    @OnDelete(action = OnDeleteAction.CASCADE) private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roommate_calendar_category_id", nullable = false)
    private RoommateCalendarCategory roommateCalendarCategory;

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @Column(name = "contents", length = 500)
    private String contents;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Builder.Default
    @ColumnDefault("false")
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    public void modify(CalendarDto.CalendarInfoDto calendarInfo) {
        this.title = calendarInfo.getTitle();
        this.contents = calendarInfo.getContents();
        this.startDate = calendarInfo.getStartDate();
        this.endDate = calendarInfo.getEndDate();
    }

    public boolean isOwner(Long memberId) {
        return Objects.equals(this.member.getId(), memberId);
    }

    public void softDelete() {
        this.isDeleted = true;
    }
}
