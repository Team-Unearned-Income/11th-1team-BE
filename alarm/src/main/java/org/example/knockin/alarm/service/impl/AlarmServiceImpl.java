package org.example.knockin.alarm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.knockin.alarm.dto.AlarmListDto;
import org.example.knockin.alarm.dto.AlarmReadAllDto;
import org.example.knockin.alarm.dto.AlarmReadDto;
import org.example.knockin.alarm.dto.AlarmSendDto;
import org.example.knockin.alarm.entity.Alarm;
import org.example.knockin.global.exception.AuthErrorCode;
import org.example.knockin.alarm.exception.AlarmErrorCode;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.alarm.repository.AlarmRepository;
import org.example.knockin.member.repository.MemberRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlarmServiceImpl {
    private final MemberRepository memberRepository;
    private final Map<Long, SseEmitter> sseEmitterMap = new ConcurrentHashMap<>();
    private final AlarmRepository alarmRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteForMembers(List<Long> memberIds, List<Long> chatRequestIds, List<Long> chatRoomIds) {
        alarmRepository.deleteForMembers(memberIds, chatRequestIds, chatRoomIds);
    }

    public SseEmitter subscribe(Long memberId) {
        memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        long timeout = 1000L * 60 * 60;
        SseEmitter sseEmitter = new SseEmitter(timeout);
        sseEmitterMap.put(memberId, sseEmitter);
        sseEmitter.onCompletion(() -> sseEmitterMap.remove(memberId, sseEmitter));
        sseEmitter.onTimeout(sseEmitter::complete);
        sseEmitter.onError(throwable -> sseEmitter.complete());
        return sseEmitter;
    }

    @Transactional
    public void sendToClient(Long memberId, String eventName, Alarm alarm) {
        alarmRepository.save(alarm);
        SseEmitter sseEmitter = sseEmitterMap.get(memberId);
        if (sseEmitter == null) return;

        try {
            String eventId = memberId + "_" + System.currentTimeMillis();
            AlarmSendDto.Response response = AlarmSendDto.Response.builder()
                    .id(alarm.getId()).title(alarm.getTitle())
                    .event(AlarmSendDto.Response.Event.builder().alarmType(alarm.getType()).eventId(alarm.getId()).build())
                    .isRead(alarm.getIsRead()).contents(alarm.getContents())
                    .createdAt(alarm.getCreatedAt()).expiredAt(alarm.getExpiredAt()).build();

            sseEmitter.send(SseEmitter.event().id(eventId).name(eventName).data(response));
        } catch (IOException | IllegalStateException e) {
            sseEmitterMap.remove(memberId, sseEmitter);
            log.debug("SSE 알림 전송 실패했습니다. memberId={}", memberId, e);
        }
    }

    public AlarmListDto.Response findAlarmList(Pageable pageable, Long memberId) {
        memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        List<AlarmListDto.Response.Alarm> alarmList = alarmRepository.findByMemberId(memberId, pageable).stream().map(item ->
                AlarmListDto.Response.Alarm.builder().id(item.getId()).title(item.getTitle()).contents(item.getContents()).isRead(item.getIsRead()).expiredAt(item.getExpiredAt()).createAt(item.getCreatedAt()).build()).toList();
        return AlarmListDto.Response.builder().alarms(alarmList).build();
    }

    @Transactional
    public AlarmReadDto.Response modifyAlarmRead(Long id, Long memberId) {
        memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        Alarm alarm = alarmRepository.findByIdAndMemberId(id, memberId).orElseThrow(() -> new BusinessException(AlarmErrorCode.ALARM_NOT_FOUND));
        alarm.readAlarm();
        return AlarmReadDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public AlarmReadAllDto.Response modifyAllAlarmRead(Long memberId) {
        memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        List<Alarm> alarmList = alarmRepository.findByMemberIdAndIsRead(memberId, false);
        alarmList.forEach(Alarm::readAlarm);
        return AlarmReadAllDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }
}
