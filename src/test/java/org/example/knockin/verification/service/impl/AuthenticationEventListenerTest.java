package org.example.knockin.verification.service.impl;

import java.time.LocalDateTime;
import org.example.knockin.alarm.entity.Alarm;
import org.example.knockin.alarm.entity.AlarmSettingType;
import org.example.knockin.alarm.entity.AlarmType;
import org.example.knockin.alarm.service.impl.AlarmServiceImpl;
import org.example.knockin.alarm.service.impl.PushNotificationServiceImpl;
import org.example.knockin.global.entity.AuthenticateAlarmTemplate;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.MemberErrorCode;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.example.knockin.verification.service.VerificationDeleteEvent;
import org.example.knockin.verification.service.VerificationSaveEvent;
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
@DisplayName("인증 승인 및 반려 이벤트 알림 테스트")
class AuthenticationEventListenerTest {

    @Mock
    private MemberServiceImpl memberService;

    @Mock
    private AlarmServiceImpl alarmService;

    @Mock
    private PushNotificationServiceImpl pushNotificationService;

    @InjectMocks
    private AuthenticationEventListener authenticationEventListener;

    @Test
    @DisplayName("인증 승인 회원에게 하루 동안 유효한 승인 알림과 푸시 알림을 전송한다")
    void sendsApprovalNotifications() {
        Member member = Member.builder().id(10L).fcmToken("token").build();
        VerificationSaveEvent event = new VerificationSaveEvent(member.getId());
        given(memberService.findByIdOrThrow(event.memberId())).willReturn(member);
        LocalDateTime earliestExpiration = LocalDateTime.now().plusDays(1);

        authenticationEventListener.handle(event);

        LocalDateTime latestExpiration = LocalDateTime.now().plusDays(1);
        ArgumentCaptor<Alarm> alarmCaptor = ArgumentCaptor.forClass(Alarm.class);
        verify(memberService).findByIdOrThrow(event.memberId());
        verify(alarmService).sendToClient(
                eq(member.getId()), eq(AuthenticateAlarmTemplate.SAVE_VERIFICATION.name()), alarmCaptor.capture());
        assertAlarm(alarmCaptor.getValue(), member, AuthenticateAlarmTemplate.SAVE_VERIFICATION.formatTitle(),
                AuthenticateAlarmTemplate.SAVE_VERIFICATION.formatContents(), earliestExpiration, latestExpiration);
        verify(pushNotificationService).send(
                member, AlarmSettingType.NOTIFICATION, AuthenticateAlarmTemplate.SAVE_VERIFICATION);
    }

    @Test
    @DisplayName("인증 반려 회원에게 반려 사유를 포함한 알림과 푸시 알림을 전송한다")
    void sendsRejectionNotificationsWithReason() {
        Member member = Member.builder().id(20L).fcmToken("token").build();
        String rejectReason = "재직 증빙을 확인할 수 없습니다.";
        VerificationDeleteEvent event = new VerificationDeleteEvent(member.getId(), rejectReason);
        given(memberService.findByIdOrThrow(event.memberId())).willReturn(member);
        LocalDateTime earliestExpiration = LocalDateTime.now().plusDays(1);

        authenticationEventListener.handle(event);

        LocalDateTime latestExpiration = LocalDateTime.now().plusDays(1);
        ArgumentCaptor<Alarm> alarmCaptor = ArgumentCaptor.forClass(Alarm.class);
        verify(memberService).findByIdOrThrow(event.memberId());
        verify(alarmService).sendToClient(
                eq(member.getId()), eq(AuthenticateAlarmTemplate.DELETE_VERIFICATION.name()), alarmCaptor.capture());
        assertAlarm(alarmCaptor.getValue(), member, AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatTitle(),
                AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatContents(rejectReason),
                earliestExpiration, latestExpiration);
        verify(pushNotificationService).send(
                member, AlarmSettingType.NOTIFICATION,
                AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatTitle(),
                AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatContents(rejectReason),
                AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatDeepLink());
    }

    @Test
    @DisplayName("승인 이벤트의 회원이 없으면 회원 미존재 예외가 발생하고 알림을 전송하지 않는다")
    void doesNotSendApprovalNotificationsWhenMemberNotFound() {
        VerificationSaveEvent event = new VerificationSaveEvent(10L);
        given(memberService.findByIdOrThrow(event.memberId()))
                .willThrow(new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        assertThatThrownBy(() -> authenticationEventListener.handle(event))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.MEMBER_NOT_FOUND);

        verify(memberService).findByIdOrThrow(event.memberId());
        verifyNoInteractions(alarmService, pushNotificationService);
    }

    @Test
    @DisplayName("반려 이벤트의 회원이 없으면 회원 미존재 예외가 발생하고 알림을 전송하지 않는다")
    void doesNotSendRejectionNotificationsWhenMemberNotFound() {
        VerificationDeleteEvent event = new VerificationDeleteEvent(20L, "반려 사유");
        given(memberService.findByIdOrThrow(event.memberId()))
                .willThrow(new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        assertThatThrownBy(() -> authenticationEventListener.handle(event))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.MEMBER_NOT_FOUND);

        verify(memberService).findByIdOrThrow(event.memberId());
        verifyNoInteractions(alarmService, pushNotificationService);
    }

    private void assertAlarm(Alarm alarm, Member member, String title, String contents,
            LocalDateTime earliestExpiration, LocalDateTime latestExpiration) {
        assertThat(alarm.getMember()).isSameAs(member);
        assertThat(alarm.getTitle()).isEqualTo(title);
        assertThat(alarm.getContents()).isEqualTo(contents);
        assertThat(alarm.getIsRead()).isFalse();
        assertThat(alarm.getType()).isEqualTo(AlarmType.DEFAULT);
        assertThat(alarm.getExpiredAt()).isBetween(earliestExpiration, latestExpiration);
    }
}
