package org.example.knockin.inquiry.service.impl;

import java.time.LocalDateTime;
import org.example.knockin.alarm.entity.Alarm;
import org.example.knockin.alarm.entity.AlarmSettingType;
import org.example.knockin.alarm.entity.AlarmType;
import org.example.knockin.alarm.service.impl.AlarmServiceImpl;
import org.example.knockin.alarm.service.impl.PushNotificationServiceImpl;
import org.example.knockin.global.entity.InquiryAlarmTemplate;
import org.example.knockin.inquiry.service.InquiryReplyEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("문의 답변 이벤트 알림 테스트")
class InquiryEventListenerTest {

    @Mock
    private AlarmServiceImpl alarmService;

    @Mock
    private PushNotificationServiceImpl pushNotificationService;

    @InjectMocks
    private InquiryEventListener inquiryEventListener;

    @Test
    @DisplayName("문의 작성자에게 하루 동안 유효한 답변 알림과 푸시 알림을 전송한다")
    void notifyAlarmSendsReplyNotifications() {
        Long memberId = 10L;
        InquiryReplyEvent event = new InquiryReplyEvent(memberId, 100L);
        LocalDateTime earliestExpiration = LocalDateTime.now().plusDays(1);

        inquiryEventListener.notifyAlarm(event);

        LocalDateTime latestExpiration = LocalDateTime.now().plusDays(1);
        ArgumentCaptor<Alarm> alarmCaptor = ArgumentCaptor.forClass(Alarm.class);
        verify(alarmService).sendToClient(
                eq(memberId), eq(InquiryAlarmTemplate.INQUIRIE_REPLY.name()), alarmCaptor.capture());
        Alarm alarm = alarmCaptor.getValue();
        assertThat(alarm.getMemberId()).isEqualTo(memberId);
        assertThat(alarm.getTitle()).isEqualTo(InquiryAlarmTemplate.INQUIRIE_REPLY.formatTitle());
        assertThat(alarm.getContents()).isEqualTo(InquiryAlarmTemplate.INQUIRIE_REPLY.formatContents());
        assertThat(alarm.getIsRead()).isFalse();
        assertThat(alarm.getType()).isEqualTo(AlarmType.DEFAULT);
        assertThat(alarm.getExpiredAt()).isBetween(earliestExpiration, latestExpiration);
        verify(pushNotificationService).send(
                memberId, AlarmSettingType.NOTIFICATION, InquiryAlarmTemplate.INQUIRIE_REPLY);
    }
}
