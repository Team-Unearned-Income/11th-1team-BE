package org.example.knockin.verification.service.impl;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.example.knockin.alarm.entity.Alarm;
import org.example.knockin.alarm.entity.AlarmSettingType;
import org.example.knockin.alarm.entity.AlarmType;
import org.example.knockin.alarm.service.impl.AlarmServiceImpl;
import org.example.knockin.alarm.service.impl.PushNotificationServiceImpl;
import org.example.knockin.global.entity.AuthenticateAlarmTemplate;
import org.example.knockin.verification.service.VerificationDeleteEvent;
import org.example.knockin.verification.service.VerificationSaveEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AuthenticationEventListener {

    private final AlarmServiceImpl alarmService;
    private final PushNotificationServiceImpl pushNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(VerificationSaveEvent event) {
        Long memberId = event.memberId();
        Alarm alarm = Alarm.builder()
                .title(AuthenticateAlarmTemplate.SAVE_VERIFICATION.formatTitle())
                .contents(AuthenticateAlarmTemplate.SAVE_VERIFICATION.formatContents())
                .isRead(false)
                .memberId(memberId)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .type(AlarmType.DEFAULT)
                .build();

        alarmService.sendToClient(memberId, AuthenticateAlarmTemplate.SAVE_VERIFICATION.name(), alarm);
        pushNotificationService.send(memberId, AlarmSettingType.NOTIFICATION, AuthenticateAlarmTemplate.SAVE_VERIFICATION);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(VerificationDeleteEvent event) {
        String rejectReason = event.rejectReason();
        Long memberId = event.memberId();
        Alarm alarm = Alarm.builder()
                .title(AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatTitle())
                .contents(AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatContents(rejectReason))
                .isRead(false)
                .memberId(memberId)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .type(AlarmType.DEFAULT)
                .build();

        alarmService.sendToClient(memberId, AuthenticateAlarmTemplate.DELETE_VERIFICATION.name(), alarm);
        pushNotificationService.send(memberId, AlarmSettingType.NOTIFICATION, AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatTitle(), AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatContents(rejectReason), AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatDeepLink());
    }
}
