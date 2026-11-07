package org.example.knockin.verification.service.impl;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.example.knockin.alarm.entity.Alarm;
import org.example.knockin.alarm.entity.AlarmSettingType;
import org.example.knockin.alarm.entity.AlarmType;
import org.example.knockin.alarm.service.impl.AlarmServiceImpl;
import org.example.knockin.alarm.service.impl.PushNotificationServiceImpl;
import org.example.knockin.global.entity.AuthenticateAlarmTemplate;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.example.knockin.verification.service.VerificationDeleteEvent;
import org.example.knockin.verification.service.VerificationSaveEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AuthenticationEventListener {

    public final MemberServiceImpl memberService;
    private final AlarmServiceImpl alarmService;
    private final PushNotificationServiceImpl pushNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(VerificationSaveEvent event) {
        Member member = memberService.findByIdOrThrow(event.memberId());
        Alarm alarm = Alarm.builder()
                .title(AuthenticateAlarmTemplate.SAVE_VERIFICATION.formatTitle())
                .contents(AuthenticateAlarmTemplate.SAVE_VERIFICATION.formatContents())
                .isRead(false)
                .member(member)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .type(AlarmType.DEFAULT)
                .build();

        alarmService.sendToClient(member.getId(), AuthenticateAlarmTemplate.SAVE_VERIFICATION.name(), alarm);
        pushNotificationService.send(member, AlarmSettingType.NOTIFICATION, AuthenticateAlarmTemplate.SAVE_VERIFICATION);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(VerificationDeleteEvent event) {
        String rejectReason = event.rejectReason();
        Member member = memberService.findByIdOrThrow(event.memberId());
        Alarm alarm = Alarm.builder()
                .title(AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatTitle())
                .contents(AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatContents(rejectReason))
                .isRead(false)
                .member(member)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .type(AlarmType.DEFAULT)
                .build();

        alarmService.sendToClient(member.getId(), AuthenticateAlarmTemplate.DELETE_VERIFICATION.name(), alarm);
        pushNotificationService.send(member, AlarmSettingType.NOTIFICATION, AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatTitle(), AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatContents(rejectReason), AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatDeepLink());
    }
}
