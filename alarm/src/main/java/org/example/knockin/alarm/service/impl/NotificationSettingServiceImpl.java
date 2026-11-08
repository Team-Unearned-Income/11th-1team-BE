package org.example.knockin.alarm.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.knockin.alarm.dto.AlarmSettingDto;
import org.example.knockin.alarm.dto.MyNotificationSettingsDto;
import org.example.knockin.alarm.entity.AlarmSetting;
import org.example.knockin.global.exception.AuthErrorCode;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.alarm.repository.AlarmSettingRepository;
import org.example.knockin.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationSettingServiceImpl {
    private final MemberRepository memberRepository;
    private final AlarmSettingRepository alarmSettingRepository;

    public MyNotificationSettingsDto.Response findAlaramSettingList(Long memberId) {
        memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        List<MyNotificationSettingsDto.Response.AlarmSettingItem> alarmSettingList = alarmSettingRepository.findByMemberId(memberId).stream().map(item -> MyNotificationSettingsDto.Response.AlarmSettingItem.builder().id(item.getId()).name(item.getAlarmSettingType().getMessage()).isEnable(item.getIsEnabled()).build()).toList();
        return MyNotificationSettingsDto.Response.builder().alarmsSettings(alarmSettingList).build();
    }

    @Transactional
    public AlarmSettingDto.Response modifyAlarmSetting(AlarmSettingDto.Request request, Long memberId) {
        memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        AlarmSetting alarmSetting = alarmSettingRepository.findByIdAndMemberId(request.getSettingId(), memberId);
        alarmSetting.updateEnable(request.getEnabled());
        return AlarmSettingDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }
}
