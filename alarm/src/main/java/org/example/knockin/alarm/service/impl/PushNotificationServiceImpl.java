package org.example.knockin.alarm.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.knockin.alarm.entity.AlarmSettingType;
import org.example.knockin.member.entity.Member;
import org.example.knockin.alarm.repository.AlarmSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PushNotificationServiceImpl {
    private final AlarmSettingRepository alarmSettingRepository;
    private final FcmServiceImpl fcmService;

    public void send(Member receiver, AlarmSettingType alarmSettingType, String title, String body, String deepLink) {
        String fcmToken = receiver.getFcmToken();
        if (!StringUtils.hasText(fcmToken)) return;
        boolean isEnabled = alarmSettingRepository.existsByMemberAndAlarmSettingTypeAndIsEnabledTrue(receiver, alarmSettingType);
        if (!isEnabled) return;
        fcmService.send(title, body, fcmToken, deepLink);
    }
}
