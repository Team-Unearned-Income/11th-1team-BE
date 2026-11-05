package org.example.knockin.alarm.entity;

import org.example.knockin.mate.entity.RoommateMatchingRequired;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "roommate_matching_required_alarm")
@DiscriminatorValue(AlarmType.Values.ROOM_MATCHING)
@OnDelete(action = OnDeleteAction.CASCADE)
public class RoommateMatchingRequiredAlarm extends Alarm{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roommate_matching_required_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private RoommateMatchingRequired roommateMatchingRequired;
}
