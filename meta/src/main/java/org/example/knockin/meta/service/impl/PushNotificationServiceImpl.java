package org.example.knockin.meta.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.knockin.global.entity.AlarmMessageTemplate;
import org.example.knockin.meta.entity.AlarmSettingType;
import org.example.knockin.member.entity.Member;
import org.example.knockin.meta.repository.AlarmSettingRepository;
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

    public void send(Member member, AlarmSettingType alarmSettingType, AlarmMessageTemplate template) {
        send(member, alarmSettingType, template.formatTitle(), template.formatContents(), template.formatDeepLink());
    }
}
