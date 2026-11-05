package org.example.knockin.meta.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.knockin.meta.dto.AlarmSettingDto;
import org.example.knockin.meta.dto.MyNotificationSettingsDto;
import org.example.knockin.meta.entity.AlarmSetting;
import org.example.knockin.member.entity.Member;
import org.example.knockin.global.exception.AuthErrorCode;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.meta.repository.AlarmSettingRepository;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationSettingServiceImpl {
    private final MemberServiceImpl memberService;
    private final AlarmSettingRepository alarmSettingRepository;

    public MyNotificationSettingsDto.Response findAlaramSettingList(Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        List<MyNotificationSettingsDto.Response.AlarmSettingItem> alarmSettingList = alarmSettingRepository.findByMember(member).stream().map(item -> MyNotificationSettingsDto.Response.AlarmSettingItem.builder().id(item.getId()).name(item.getAlarmSettingType().getMessage()).isEnable(item.getIsEnabled()).build()).toList();
        return MyNotificationSettingsDto.Response.builder().alarmsSettings(alarmSettingList).build();
    }

    @Transactional
    public AlarmSettingDto.Response modifyAlarmSetting(AlarmSettingDto.Request request, Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        AlarmSetting alarmSetting = alarmSettingRepository.findByIdAndMember(request.getSettingId(), member);
        alarmSetting.updateEnable(request.getEnabled());
        return AlarmSettingDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }
}
