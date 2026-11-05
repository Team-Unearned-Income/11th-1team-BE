package org.example.knockin.alarm.service.impl;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.example.knockin.member.service.impl.BasicInformationServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.example.knockin.alarm.entity.AlarmType;
import org.example.knockin.chat.entity.ChattingRequired;
import org.example.knockin.alarm.entity.ChattingRequiredAlarm;
import org.example.knockin.member.entity.BasicInformation;
import org.example.knockin.member.entity.Member;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChattingRequiredAlarmServiceImpl {
    private final BasicInformationServiceImpl basicInformationService;
    private final AlarmServiceImpl alarmService;
    @Value("${policy.request-alarm.expire-days}")
    private int requestAlarmExpireDays;

    public void send(Member receiver, Member actor, ChattingRequired chattingRequired, String alarmTemplate) {
        BasicInformation basicInformation = basicInformationService.findLatestBasicInformation(actor);
        String actorName = basicInformation.getName();
        String message = String.format(alarmTemplate, actorName);

        ChattingRequiredAlarm alarm = ChattingRequiredAlarm.builder()
                .member(receiver)
                .title(message)
                .contents(message)
                .isRead(false)
                .expiredAt(LocalDateTime.now().plusDays(requestAlarmExpireDays))
                .type(AlarmType.CHATTING_REQUIRED)
                .chattingRequired(chattingRequired)
                .build();

        alarmService.sendToClient(receiver.getId(), AlarmType.CHATTING_REQUIRED.name(), alarm);
    }
}
