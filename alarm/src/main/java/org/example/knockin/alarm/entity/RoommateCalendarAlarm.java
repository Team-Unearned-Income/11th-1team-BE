package org.example.knockin.alarm.entity;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.example.knockin.alarm.entity.AlarmType.Values;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "roommate_calendar_alarm")
@DiscriminatorValue(Values.ROOMMATE_CALENDAR)
@OnDelete(action = OnDeleteAction.CASCADE)
public class RoommateCalendarAlarm extends Alarm {
    @Column(name = "roommate_calendar_id", nullable = false)
    private Long roommateCalendarId;
}
