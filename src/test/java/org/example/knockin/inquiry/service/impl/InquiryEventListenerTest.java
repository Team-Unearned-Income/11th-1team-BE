package org.example.knockin.inquiry.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;
import org.example.knockin.alarm.entity.Alarm;
import org.example.knockin.alarm.entity.AlarmSettingType;
import org.example.knockin.alarm.entity.AlarmType;
import org.example.knockin.alarm.service.impl.AlarmServiceImpl;
import org.example.knockin.alarm.service.impl.PushNotificationServiceImpl;
import org.example.knockin.global.entity.InquiryAlarmTemplate;
import org.example.knockin.global.exception.AuthErrorCode;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.inquiry.service.InquiryReplyEvent;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("문의 답변 이벤트 알림 테스트")
class InquiryEventListenerTest {

    @Mock
    private MemberServiceImpl memberService;

    @Mock
    private AlarmServiceImpl alarmService;

    @Mock
    private PushNotificationServiceImpl pushNotificationService;

    @InjectMocks
    private InquiryEventListener inquiryEventListener;

    @Test
    @DisplayName("문의 작성자에게 하루 동안 유효한 답변 알림과 푸시 알림을 전송한다")
    void notifyAlarmSendsReplyNotifications() {
        Member member = Member.builder().id(10L).fcmToken("token").build();
        InquiryReplyEvent event = new InquiryReplyEvent(member.getId(), 100L);
        given(memberService.findById(member.getId())).willReturn(Optional.of(member));
        LocalDateTime earliestExpiration = LocalDateTime.now().plusDays(1);

        inquiryEventListener.notifyAlarm(event);

        LocalDateTime latestExpiration = LocalDateTime.now().plusDays(1);
        ArgumentCaptor<Alarm> alarmCaptor = ArgumentCaptor.forClass(Alarm.class);
        verify(memberService).findById(event.memberId());
        verify(alarmService).sendToClient(
                eq(member.getId()), eq(InquiryAlarmTemplate.INQUIRIE_REPLY.name()), alarmCaptor.capture());
        Alarm alarm = alarmCaptor.getValue();
        assertThat(alarm.getMemberId()).isEqualTo(member.getId());
        assertThat(alarm.getTitle()).isEqualTo(InquiryAlarmTemplate.INQUIRIE_REPLY.formatTitle());
        assertThat(alarm.getContents()).isEqualTo(InquiryAlarmTemplate.INQUIRIE_REPLY.formatContents());
        assertThat(alarm.getIsRead()).isFalse();
        assertThat(alarm.getType()).isEqualTo(AlarmType.DEFAULT);
        assertThat(alarm.getExpiredAt()).isBetween(earliestExpiration, latestExpiration);
        verify(pushNotificationService).send(
                member, AlarmSettingType.NOTIFICATION, InquiryAlarmTemplate.INQUIRIE_REPLY);
    }

    @Test
    @DisplayName("문의 작성자가 없으면 회원 미존재 예외가 발생하고 알림을 전송하지 않는다")
    void notifyAlarmFailsWhenMemberNotFound() {
        InquiryReplyEvent event = new InquiryReplyEvent(10L, 100L);
        given(memberService.findById(event.memberId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryEventListener.notifyAlarm(event))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.MEMBER_NOT_FOUND);

        verifyNoInteractions(alarmService, pushNotificationService);
    }
}
