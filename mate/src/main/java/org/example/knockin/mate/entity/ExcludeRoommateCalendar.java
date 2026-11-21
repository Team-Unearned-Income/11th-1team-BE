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
@Table(name = "exclude_roommate_calendar")
public class ExcludeRoommateCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repeat_roommate_calendar_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private RepeatRoommateCalendar repeatRoommateCalendar;

    @Column(name = "exclude_at")
    private LocalDateTime excludeAt;
}
