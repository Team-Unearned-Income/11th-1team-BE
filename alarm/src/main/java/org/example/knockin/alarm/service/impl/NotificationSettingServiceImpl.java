package org.example.knockin.alarm.service.impl;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.example.knockin.alarm.dto.AlarmSettingDto;
import org.example.knockin.alarm.dto.MyNotificationSettingsDto;
import org.example.knockin.alarm.entity.AlarmSetting;
import org.example.knockin.alarm.entity.AlarmSettingType;
import org.example.knockin.alarm.repository.AlarmSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationSettingServiceImpl {
    private final AlarmSettingRepository alarmSettingRepository;

    public MyNotificationSettingsDto.Response findAlaramSettingList(Long memberId) {
        List<MyNotificationSettingsDto.Response.AlarmSettingItem> alarmSettingList = alarmSettingRepository.findByMemberId(memberId).stream().map(item -> MyNotificationSettingsDto.Response.AlarmSettingItem.builder().id(item.getId()).name(item.getAlarmSettingType().getMessage()).isEnable(item.getIsEnabled()).build()).toList();
        return MyNotificationSettingsDto.Response.builder().alarmsSettings(alarmSettingList).build();
    }

    @Transactional
    public AlarmSettingDto.Response modifyAlarmSetting(AlarmSettingDto.Request request, Long memberId) {
        AlarmSetting alarmSetting = alarmSettingRepository.findByIdAndMemberId(request.getSettingId(), memberId);
        alarmSetting.updateEnable(request.getEnabled());
        return AlarmSettingDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    public void saveAllAlarmSettingsByMemberId(Long memberId) {
        List<AlarmSetting> alarmSettingList = Arrays.stream(AlarmSettingType.values())
                .map(item -> AlarmSetting.builder().memberId(memberId).isEnabled(true).alarmSettingType(item).build()).toList();
        alarmSettingRepository.saveAll(alarmSettingList);
    }
}
