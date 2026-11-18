package org.example.knockin.alarm.service.impl;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.knockin.alarm.entity.AlarmSettingType;
import org.example.knockin.alarm.port.FcmTokenReader;
import org.example.knockin.global.entity.AlarmMessageTemplate;
import org.example.knockin.member.entity.Member;
import org.example.knockin.alarm.repository.AlarmSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PushNotificationServiceImpl {
    private final AlarmSettingRepository alarmSettingRepository;
    private final FcmServiceImpl fcmService;
    private final FcmTokenReader fcmTokenReader;

    @Deprecated
    public void send(Member receiver, AlarmSettingType alarmSettingType, String title, String body, String deepLink) {
        String fcmToken = receiver.getFcmToken();
        if (!StringUtils.hasText(fcmToken)) return;
        boolean isEnabled = alarmSettingRepository.existsByMemberIdAndAlarmSettingTypeAndIsEnabledTrue(receiver.getId(), alarmSettingType);
        if (!isEnabled) return;
        fcmService.send(title, body, fcmToken, deepLink);
    }

    @Deprecated
    public void send(Member receiver, AlarmSettingType alarmSettingType, AlarmMessageTemplate alarmMessageTemplate) {
        send(receiver, alarmSettingType, alarmMessageTemplate.formatTitle(), alarmMessageTemplate.formatContents(), alarmMessageTemplate.formatDeepLink());
    }

    public void send(Long receiverId, AlarmSettingType alarmSettingType, String title, String body, String deepLink) {
        Optional<String> fcmToken = fcmTokenReader.readFcmTokenById(receiverId);
        if (fcmToken.isEmpty()) return;

        if (!StringUtils.hasText(fcmToken.get())) return;
        boolean isEnabled = alarmSettingRepository.existsByMemberIdAndAlarmSettingTypeAndIsEnabledTrue(receiverId, alarmSettingType);
        if (!isEnabled) return;
        fcmService.send(title, body, fcmToken.get(), deepLink);
    }

    public void send(Long receiverId, AlarmSettingType alarmSettingType, AlarmMessageTemplate alarmMessageTemplate) {
        send(receiverId, alarmSettingType, alarmMessageTemplate.formatTitle(), alarmMessageTemplate.formatContents(), alarmMessageTemplate.formatDeepLink());
    }
}
