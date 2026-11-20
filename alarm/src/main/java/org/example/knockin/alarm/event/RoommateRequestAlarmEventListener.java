package org.example.knockin.alarm.event;

import lombok.RequiredArgsConstructor;
import org.example.knockin.alarm.entity.AlarmSettingType;
import org.example.knockin.alarm.service.impl.PushNotificationServiceImpl;
import org.example.knockin.alarm.service.impl.RoommateMatchingRequiredAlarmServiceImpl;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.MemberErrorCode;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.repository.MemberRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoommateRequestAlarmEventListener {
    private final MemberRepository memberRepository;
    private final RoommateMatchingRequiredAlarmServiceImpl roommateMatchingRequiredAlarmService;
    private final PushNotificationServiceImpl pushNotificationService;

    // 기존 알림 -> 푸시 -> 소켓 순서와 실패 전파를 유지하기 위해 동기 처리한다.
    @EventListener
    public void handle(RoommateRequestAlarmEvent event) {
        Member receiver = memberRepository.findById(event.getReceiverId())
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
        roommateMatchingRequiredAlarmService.send(receiver, event.getTitle(), event.getContents(), event.getRequestId());
        pushNotificationService.send(receiver, AlarmSettingType.NOTIFICATION,
                event.getTitle(), event.getContents(), event.getDeepLink());
        event.complete();
    }
}
