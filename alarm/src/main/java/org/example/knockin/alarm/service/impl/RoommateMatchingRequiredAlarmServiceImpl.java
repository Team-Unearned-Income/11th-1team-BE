package org.example.knockin.alarm.service.impl;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.example.knockin.alarm.entity.AlarmType;
import org.example.knockin.member.entity.Member;
import org.example.knockin.mate.entity.RoommateMatchingRequired;
import org.example.knockin.alarm.entity.RoommateMatchingRequiredAlarm;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoommateMatchingRequiredAlarmServiceImpl {
    private final AlarmServiceImpl alarmService;
    @Value("${policy.request-alarm.expire-days}")
    private int requestAlarmExpireDays;

    public void send(Member receiver, String title, String contents, RoommateMatchingRequired required) {
        RoommateMatchingRequiredAlarm alarm = RoommateMatchingRequiredAlarm.builder()
                .memberId(receiver.getId())
                .title(title)
                .contents(contents)
                .expiredAt(LocalDateTime.now().plusDays(requestAlarmExpireDays))
                .type(AlarmType.ROOM_MATCHING)
                .roommateMatchingRequiredId(required.getId())
                .build();

        alarmService.sendToClient(receiver.getId(), AlarmType.ROOM_MATCHING.name(), alarm);
    }
}
