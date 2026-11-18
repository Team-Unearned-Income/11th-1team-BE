package org.example.knockin.service.impl;

import org.example.knockin.alarm.dto.AlarmSettingDto;
import org.example.knockin.alarm.dto.MyNotificationSettingsDto;
import org.example.knockin.alarm.entity.AlarmSetting;
import org.example.knockin.alarm.entity.AlarmSettingType;
import org.example.knockin.alarm.repository.AlarmSettingRepository;
import org.example.knockin.alarm.service.impl.NotificationSettingServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 설정 서비스 테스트")
class NotificationSettingServiceImplTest {

    @Mock
    private AlarmSettingRepository alarmSettingRepository;

    @Captor
    private ArgumentCaptor<List<AlarmSetting>> settingsCaptor;

    @InjectMocks
    private NotificationSettingServiceImpl notificationSettingService;

    @Test
    @DisplayName("회원의 알림 설정 목록 조회 성공 테스트")
    void findAlarmSettingListSuccessTest() {
        // given
        Long memberId = 1L;
        AlarmSetting setting = AlarmSetting.builder()
                .id(100L)
                .alarmSettingType(AlarmSettingType.NOTIFICATION)
                .isEnabled(true)
                .build();

        given(alarmSettingRepository.findByMemberId(memberId)).willReturn(List.of(setting));

        // when
        MyNotificationSettingsDto.Response response = notificationSettingService.findAlaramSettingList(memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getAlarmsSettings()).hasSize(1);
        MyNotificationSettingsDto.Response.AlarmSettingItem item = response.getAlarmsSettings().get(0);
        assertThat(item.getId()).isEqualTo(100L);
        assertThat(item.getName()).isEqualTo(AlarmSettingType.NOTIFICATION.getMessage());
        assertThat(item.getIsEnable()).isTrue();
    }

    @Test
    @DisplayName("회원의 알림 설정이 없으면 빈 목록을 반환한다")
    void returnsEmptyListWhenMemberHasNoSettings() {
        Long memberId = 1L;
        given(alarmSettingRepository.findByMemberId(memberId)).willReturn(List.of());

        assertThat(notificationSettingService.findAlaramSettingList(memberId).getAlarmsSettings()).isEmpty();
    }

    @Test
    @DisplayName("알림 설정 상태 수정 성공 테스트")
    void modifyAlarmSettingSuccessTest() {
        // given
        Long memberId = 1L;
        AlarmSettingDto.Request request = new AlarmSettingDto.Request(100L, false);

        AlarmSetting setting = spy(AlarmSetting.builder()
                .id(100L)
                .alarmSettingType(AlarmSettingType.NOTIFICATION)
                .isEnabled(true)
                .build());

        given(alarmSettingRepository.findByIdAndMemberId(100L, memberId)).willReturn(setting);

        // when
        AlarmSettingDto.Response response = notificationSettingService.modifyAlarmSetting(request, memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(setting).updateEnable(false);
    }

    @Test
    @DisplayName("새 회원의 모든 알림 유형을 활성화하여 저장한다")
    void savesEnabledSettingsForAllAlarmTypes() {
        Long memberId = 1L;

        notificationSettingService.saveAllAlarmSettingsByMemberId(memberId);

        verify(alarmSettingRepository).saveAll(settingsCaptor.capture());
        assertThat(settingsCaptor.getValue())
                .allSatisfy(setting -> {
                    assertThat(setting.getMemberId()).isEqualTo(memberId);
                    assertThat(setting.getIsEnabled()).isTrue();
                })
                .extracting(AlarmSetting::getAlarmSettingType)
                .containsExactlyInAnyOrder(AlarmSettingType.values());
    }
}
