package org.example.knockin.mate.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "repeat_roommate_calendar")
public class RepeatRoommateCalendar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roommate_calendar_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private RoommateCalendar roommateCalendar;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "repeat_type")
    @Enumerated(EnumType.STRING)
    private RepeatType repeatType;

    public void modify(LocalDateTime endDate, RepeatType repeatType) {
        this.endDate = endDate;
        this.repeatType = repeatType;
    }
}
