package org.example.knockin.inquiry.service.impl;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.example.knockin.global.entity.InquiryAlarmTemplate;
import org.example.knockin.global.exception.AuthErrorCode;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.inquiry.service.InquiryReplyEvent;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.service.impl.MemberServiceImpl;
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

    private final MemberServiceImpl memberService;
    private final AlarmServiceImpl alarmService;
    private final PushNotificationServiceImpl pushNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyAlarm(InquiryReplyEvent event) {
        Member member = memberService.findById(event.memberId()).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        Alarm alarm = Alarm.builder()
                .title(InquiryAlarmTemplate.INQUIRIE_REPLY.formatTitle())
                .contents(InquiryAlarmTemplate.INQUIRIE_REPLY.formatContents())
                .isRead(false)
                .memberId(member.getId())
                .expiredAt(LocalDateTime.now().plusDays(1))
                .type(AlarmType.DEFAULT)
                .build();

        alarmService.sendToClient(member.getId(), InquiryAlarmTemplate.INQUIRIE_REPLY.name(), alarm);
        pushNotificationService.send(member, AlarmSettingType.NOTIFICATION, InquiryAlarmTemplate.INQUIRIE_REPLY);
    }
}
