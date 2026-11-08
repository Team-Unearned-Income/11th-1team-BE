package org.example.knockin.service.impl;

import org.example.knockin.alarm.dto.AlarmSettingDto;
import org.example.knockin.alarm.dto.MyNotificationSettingsDto;
import org.example.knockin.alarm.entity.AlarmSetting;
import org.example.knockin.alarm.entity.AlarmSettingType;
import org.example.knockin.alarm.service.impl.NotificationSettingServiceImpl;
import org.example.knockin.member.entity.Member;
import org.example.knockin.global.exception.AuthErrorCode;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.alarm.repository.AlarmSettingRepository;
import org.example.knockin.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 설정 서비스 테스트")
class NotificationSettingServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AlarmSettingRepository alarmSettingRepository;

    @InjectMocks
    private NotificationSettingServiceImpl notificationSettingService;

    @Test
    @DisplayName("회원의 알림 설정 목록 조회 성공 테스트")
    void findAlarmSettingListSuccessTest() {
        // given
        Long memberId = 1L;
        Member member = mock(Member.class);
        AlarmSetting setting = AlarmSetting.builder()
                .id(100L)
                .alarmSettingType(AlarmSettingType.NOTIFICATION)
                .isEnabled(true)
                .build();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
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
    @DisplayName("알림 설정 목록 조회 시 회원이 없으면 BusinessException 발생")
    void findAlarmSettingListMemberNotFoundTest() {
        // given
        Long memberId = 1L;
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationSettingService.findAlaramSettingList(memberId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.MEMBER_NOT_FOUND);

        verifyNoInteractions(alarmSettingRepository);
    }

    @Test
    @DisplayName("알림 설정 상태 수정 성공 테스트")
    void modifyAlarmSettingSuccessTest() {
        // given
        Long memberId = 1L;
        Member member = mock(Member.class);
        AlarmSettingDto.Request request = new AlarmSettingDto.Request(100L, false);

        AlarmSetting setting = spy(AlarmSetting.builder()
                .id(100L)
                .alarmSettingType(AlarmSettingType.NOTIFICATION)
                .isEnabled(true)
                .build());

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(alarmSettingRepository.findByIdAndMemberId(100L, memberId)).willReturn(setting);

        // when
        AlarmSettingDto.Response response = notificationSettingService.modifyAlarmSetting(request, memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(setting).updateEnable(false);
    }

    @Test
    @DisplayName("알림 설정 상태 수정 시 회원이 없으면 BusinessException 발생")
    void modifyAlarmSettingMemberNotFoundTest() {
        // given
        Long memberId = 1L;
        AlarmSettingDto.Request request = new AlarmSettingDto.Request(100L, false);
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationSettingService.modifyAlarmSetting(request, memberId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.MEMBER_NOT_FOUND);

        verifyNoInteractions(alarmSettingRepository);
    }
}
