package org.example.knockin.member.service.impl;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.example.knockin.alarm.entity.Alarm;
import org.example.knockin.alarm.entity.AlarmSettingType;
import org.example.knockin.alarm.entity.AlarmType;
import org.example.knockin.alarm.service.impl.AlarmServiceImpl;
import org.example.knockin.alarm.service.impl.PushNotificationServiceImpl;
import org.example.knockin.global.entity.MemberAlarmTemplate;
import org.example.knockin.member.service.MemberStateActiveEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MemberEventListener {

    private final AlarmServiceImpl alarmService;
    private final PushNotificationServiceImpl pushNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MemberStateActiveEvent event) {
        Long memberId = event.memberId();
        Alarm alarm = Alarm.builder()
                .title(MemberAlarmTemplate.MEMBER_ACTIVE.formatTitle())
                .contents(MemberAlarmTemplate.MEMBER_ACTIVE.formatContents())
                .isRead(false)
                .memberId(memberId)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .type(AlarmType.DEFAULT)
                .build();

        alarmService.sendToClient(memberId, MemberAlarmTemplate.MEMBER_ACTIVE.name(), alarm);
        pushNotificationService.send(memberId, AlarmSettingType.NOTIFICATION, MemberAlarmTemplate.MEMBER_ACTIVE.formatTitle(), MemberAlarmTemplate.MEMBER_ACTIVE.formatContents(), MemberAlarmTemplate.MEMBER_ACTIVE.formatDeepLink());
    }
}
