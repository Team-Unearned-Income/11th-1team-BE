package org.example.knockin.verification.service.impl;

import java.time.LocalDateTime;
import org.example.knockin.alarm.entity.Alarm;
import org.example.knockin.alarm.entity.AlarmSettingType;
import org.example.knockin.alarm.entity.AlarmType;
import org.example.knockin.alarm.service.impl.AlarmServiceImpl;
import org.example.knockin.alarm.service.impl.PushNotificationServiceImpl;
import org.example.knockin.global.entity.AuthenticateAlarmTemplate;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("인증 승인 및 반려 이벤트 알림 테스트")
class AuthenticationEventListenerTest {

    @Mock
    private AlarmServiceImpl alarmService;

    @Mock
    private PushNotificationServiceImpl pushNotificationService;

    @InjectMocks
    private AuthenticationEventListener authenticationEventListener;

    @Test
    @DisplayName("인증 승인 회원에게 하루 동안 유효한 승인 알림과 푸시 알림을 전송한다")
    void sendsApprovalNotifications() {
        Long memberId = 10L;
        VerificationSaveEvent event = new VerificationSaveEvent(memberId);
        LocalDateTime earliestExpiration = LocalDateTime.now().plusDays(1);

        authenticationEventListener.handle(event);

        LocalDateTime latestExpiration = LocalDateTime.now().plusDays(1);
        ArgumentCaptor<Alarm> alarmCaptor = ArgumentCaptor.forClass(Alarm.class);
        verify(alarmService).sendToClient(
                eq(memberId), eq(AuthenticateAlarmTemplate.SAVE_VERIFICATION.name()), alarmCaptor.capture());
        assertAlarm(alarmCaptor.getValue(), memberId, AuthenticateAlarmTemplate.SAVE_VERIFICATION.formatTitle(),
                AuthenticateAlarmTemplate.SAVE_VERIFICATION.formatContents(), earliestExpiration, latestExpiration);
        verify(pushNotificationService).send(
                memberId, AlarmSettingType.NOTIFICATION, AuthenticateAlarmTemplate.SAVE_VERIFICATION);
    }

    @Test
    @DisplayName("인증 반려 회원에게 반려 사유를 포함한 알림과 푸시 알림을 전송한다")
    void sendsRejectionNotificationsWithReason() {
        Long memberId = 20L;
        String rejectReason = "재직 증빙을 확인할 수 없습니다.";
        VerificationDeleteEvent event = new VerificationDeleteEvent(memberId, rejectReason);
        LocalDateTime earliestExpiration = LocalDateTime.now().plusDays(1);

        authenticationEventListener.handle(event);

        LocalDateTime latestExpiration = LocalDateTime.now().plusDays(1);
        ArgumentCaptor<Alarm> alarmCaptor = ArgumentCaptor.forClass(Alarm.class);
        verify(alarmService).sendToClient(
                eq(memberId), eq(AuthenticateAlarmTemplate.DELETE_VERIFICATION.name()), alarmCaptor.capture());
        assertAlarm(alarmCaptor.getValue(), memberId, AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatTitle(),
                AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatContents(rejectReason),
                earliestExpiration, latestExpiration);
        verify(pushNotificationService).send(
                memberId, AlarmSettingType.NOTIFICATION,
                AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatTitle(),
                AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatContents(rejectReason),
                AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatDeepLink());
    }

    private void assertAlarm(Alarm alarm, Long memberId, String title, String contents,
            LocalDateTime earliestExpiration, LocalDateTime latestExpiration) {
        assertThat(alarm.getMemberId()).isEqualTo(memberId);
        assertThat(alarm.getTitle()).isEqualTo(title);
        assertThat(alarm.getContents()).isEqualTo(contents);
        assertThat(alarm.getIsRead()).isFalse();
        assertThat(alarm.getType()).isEqualTo(AlarmType.DEFAULT);
        assertThat(alarm.getExpiredAt()).isBetween(earliestExpiration, latestExpiration);
    }
}
