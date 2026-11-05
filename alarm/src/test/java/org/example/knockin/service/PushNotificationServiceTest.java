package org.example.knockin.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.example.knockin.alarm.entity.AlarmSettingType;
import org.example.knockin.member.entity.Member;
import org.example.knockin.alarm.repository.AlarmSettingRepository;
import org.example.knockin.alarm.service.impl.FcmServiceImpl;
import org.example.knockin.alarm.service.impl.PushNotificationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("푸시 알림 서비스")
class PushNotificationServiceTest {

    @Mock
    private AlarmSettingRepository alarmSettingRepository;

    @Mock
    private FcmServiceImpl fcmService;

    @InjectMocks
    private PushNotificationServiceImpl pushNotificationService;

    @Test
    @DisplayName("FCM 토큰이 있고 알림 설정이 활성화되어 있으면 푸시 알림을 전송한다")
    void sendPushNotificationWhenEnabled() {
        // given
        Member receiver = member("fcm-token");
        given(alarmSettingRepository.existsByMemberAndAlarmSettingTypeAndIsEnabledTrue(
                receiver,
                AlarmSettingType.NOTIFICATION
        )).willReturn(true);

        // when
        pushNotificationService.send(
                receiver,
                AlarmSettingType.NOTIFICATION,
                "제목",
                "본문",
                "knockinrn://chat/10"
        );

        // then
        verify(fcmService).send(
                "제목",
                "본문",
                "fcm-token",
                "knockinrn://chat/10"
        );
    }

    @Test
    @DisplayName("알림 설정이 비활성화되어 있거나 없으면 푸시 알림을 전송하지 않는다")
    void doesNotSendPushNotificationWhenDisabled() {
        // given
        Member receiver = member("fcm-token");
        given(alarmSettingRepository.existsByMemberAndAlarmSettingTypeAndIsEnabledTrue(
                receiver,
                AlarmSettingType.NOTIFICATION
        )).willReturn(false);

        // when
        pushNotificationService.send(
                receiver,
                AlarmSettingType.NOTIFICATION,
                "제목",
                "본문",
                "knockinrn://chat/10"
        );

        // then
        verifyNoInteractions(fcmService);
    }

    @Test
    @DisplayName("FCM 토큰이 null이면 설정을 조회하거나 푸시 알림을 전송하지 않는다")
    void doesNotSendPushNotificationWhenTokenIsNull() {
        // given
        Member receiver = member(null);

        // when
        pushNotificationService.send(
                receiver,
                AlarmSettingType.NOTIFICATION,
                "제목",
                "본문",
                "knockinrn://chat/10"
        );

        // then
        verifyNoInteractions(alarmSettingRepository, fcmService);
    }

    @Test
    @DisplayName("FCM 토큰이 공백이면 설정을 조회하거나 푸시 알림을 전송하지 않는다")
    void doesNotSendPushNotificationWhenTokenIsBlank() {
        // given
        Member receiver = member(" ");

        // when
        pushNotificationService.send(
                receiver,
                AlarmSettingType.NOTIFICATION,
                "제목",
                "본문",
                "knockinrn://chat/10"
        );

        // then
        verifyNoInteractions(alarmSettingRepository, fcmService);
    }

    private Member member(String fcmToken) {
        return Member.builder()
                .id(1L)
                .fcmToken(fcmToken)
                .build();
    }
}
