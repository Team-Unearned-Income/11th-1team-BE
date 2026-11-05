package org.example.knockin.alarm.entity;

import org.example.knockin.chat.entity.ChattingRequired;

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
@Table(name = "chatting_required_alarm")
@DiscriminatorValue(AlarmType.Values.CHATTING_REQUIRED)
@OnDelete(action = OnDeleteAction.CASCADE)
public class ChattingRequiredAlarm extends Alarm{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatting_required_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ChattingRequired chattingRequired;
}
