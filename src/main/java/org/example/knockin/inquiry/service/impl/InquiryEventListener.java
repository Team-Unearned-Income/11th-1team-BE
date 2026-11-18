package org.example.knockin.inquiry.service.impl;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.example.knockin.global.entity.InquiryAlarmTemplate;
import org.example.knockin.inquiry.service.InquiryReplyEvent;
import org.example.knockin.alarm.entity.Alarm;
import org.example.knockin.alarm.entity.AlarmSettingType;
import org.example.knockin.alarm.entity.AlarmType;
import org.example.knockin.alarm.service.impl.AlarmServiceImpl;
import org.example.knockin.alarm.service.impl.PushNotificationServiceImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class InquiryEventListener {

    private final AlarmServiceImpl alarmService;
    private final PushNotificationServiceImpl pushNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyAlarm(InquiryReplyEvent event) {
        Long memberId = event.memberId();
        Alarm alarm = Alarm.builder()
                .title(InquiryAlarmTemplate.INQUIRIE_REPLY.formatTitle())
                .contents(InquiryAlarmTemplate.INQUIRIE_REPLY.formatContents())
                .isRead(false)
                .memberId(memberId)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .type(AlarmType.DEFAULT)
                .build();

        alarmService.sendToClient(memberId, InquiryAlarmTemplate.INQUIRIE_REPLY.name(), alarm);
        pushNotificationService.send(memberId, AlarmSettingType.NOTIFICATION, InquiryAlarmTemplate.INQUIRIE_REPLY);
    }
}
