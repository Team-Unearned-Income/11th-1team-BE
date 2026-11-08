package org.example.knockin.service.impl;

import org.example.knockin.alarm.dto.AlarmListDto;
import org.example.knockin.alarm.dto.AlarmReadAllDto;
import org.example.knockin.alarm.dto.AlarmReadDto;
import org.example.knockin.alarm.entity.Alarm;
import org.example.knockin.alarm.entity.AlarmType;
import org.example.knockin.alarm.service.impl.AlarmServiceImpl;
import org.example.knockin.member.entity.Member;
import org.example.knockin.global.exception.AuthErrorCode;
import org.example.knockin.alarm.exception.AlarmErrorCode;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.alarm.repository.AlarmRepository;
import org.example.knockin.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 서비스 테스트")
class AlarmServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AlarmRepository alarmRepository;

    @InjectMocks
    private AlarmServiceImpl alarmService;

    @Test
    @DisplayName("알림 구독 성공 테스트")
    void subscribeSuccessTest() {
        // given
        Long memberId = 1L;
        Member member = mock(Member.class);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        // when
        SseEmitter emitter = alarmService.subscribe(memberId);

        // then
        assertThat(emitter).isNotNull();
        Map<Long, SseEmitter> map = (Map<Long, SseEmitter>) ReflectionTestUtils.getField(alarmService, "sseEmitterMap");
        assertThat(map).containsKey(memberId);
    }

    @Test
    @DisplayName("알림 구독 시 회원 정보를 찾지 못하면 BusinessException 발생")
    void subscribeMemberNotFoundTest() {
        // given
        Long memberId = 1L;
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> alarmService.subscribe(memberId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("알림 전송 성공 테스트 (sendToClient)")
    void sendToClientSuccessTest() throws IOException {
        // given
        Long memberId = 1L;
        String eventName = "testEvent";

        Alarm alarm = spy(Alarm.builder()
                .id(100L)
                .title("Test Title")
                .contents("Test Contents")
                .isRead(false)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .type(AlarmType.DEFAULT)
                .build());
        ReflectionTestUtils.setField(alarm, "createdAt", LocalDateTime.now());

        SseEmitter mockEmitter = mock(SseEmitter.class);
        Map<Long, SseEmitter> map = (Map<Long, SseEmitter>) ReflectionTestUtils.getField(alarmService, "sseEmitterMap");
        map.put(memberId, mockEmitter);

        // when
        alarmService.sendToClient(memberId, eventName, alarm);

        // then
        verify(alarmRepository).save(alarm);
        verify(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("SSE 구독이 없어도 알림을 저장한다")
    void sendToClientWithoutSubscriptionTest() {
        // given
        Long memberId = 1L;
        Alarm alarm = Alarm.builder()
                .id(100L)
                .title("Test Title")
                .contents("Test Contents")
                .isRead(false)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .type(AlarmType.DEFAULT)
                .build();

        // when
        alarmService.sendToClient(memberId, "testEvent", alarm);

        // then
        verify(alarmRepository).save(alarm);
    }

    @Test
    @DisplayName("SSE 전송 실패 시 예외를 전파하지 않고 Emitter를 제거한다")
    void sendToClientFailureTest() throws IOException {
        // given
        Long memberId = 1L;
        String eventName = "testEvent";

        Alarm alarm = spy(Alarm.builder()
                .id(100L)
                .title("Test Title")
                .contents("Test Contents")
                .isRead(false)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .type(AlarmType.DEFAULT)
                .build());
        ReflectionTestUtils.setField(alarm, "createdAt", LocalDateTime.now());

        SseEmitter mockEmitter = mock(SseEmitter.class);
        doThrow(new IOException("Connection reset")).when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

        Map<Long, SseEmitter> map = (Map<Long, SseEmitter>) ReflectionTestUtils.getField(alarmService, "sseEmitterMap");
        map.put(memberId, mockEmitter);

        // when
        alarmService.sendToClient(memberId, eventName, alarm);

        // then
        verify(alarmRepository).save(alarm);
        assertThat(map).doesNotContainKey(memberId);
    }

    @Test
    @DisplayName("알림 목록 조회 성공 테스트")
    void findAlarmListSuccessTest() {
        // given
        Long memberId = 1L;
        Member member = mock(Member.class);
        Pageable pageable = PageRequest.of(0, 10);

        Alarm alarm = Alarm.builder()
                .id(100L)
                .title("Title")
                .contents("Contents")
                .isRead(false)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();
        ReflectionTestUtils.setField(alarm, "createdAt", LocalDateTime.now());

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(alarmRepository.findByMemberId(memberId, pageable)).willReturn(List.of(alarm));

        // when
        AlarmListDto.Response response = alarmService.findAlarmList(pageable, memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getAlarms()).hasSize(1);
        assertThat(response.getAlarms().get(0).getId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("단일 알림 읽음 처리 성공 테스트")
    void modifyAlarmReadSuccessTest() {
        // given
        Long memberId = 1L;
        Long alarmId = 100L;
        Member member = mock(Member.class);

        Alarm alarm = spy(Alarm.builder()
                .id(alarmId)
                .isRead(false)
                .build());

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(alarmRepository.findByIdAndMemberId(alarmId, memberId)).willReturn(Optional.of(alarm));

        // when
        AlarmReadDto.Response response = alarmService.modifyAlarmRead(alarmId, memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(alarm).readAlarm();
    }

    @Test
    @DisplayName("단일 알림 읽음 처리 시 알림을 찾지 못하면 BusinessException 발생")
    void modifyAlarmReadNotFoundTest() {
        // given
        Long memberId = 1L;
        Long alarmId = 100L;
        Member member = mock(Member.class);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(alarmRepository.findByIdAndMemberId(alarmId, memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> alarmService.modifyAlarmRead(alarmId, memberId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AlarmErrorCode.ALARM_NOT_FOUND);
    }

    @Test
    @DisplayName("전체 알림 읽음 처리 성공 테스트")
    void modifyAllAlarmReadSuccessTest() {
        // given
        Long memberId = 1L;
        Member member = mock(Member.class);

        Alarm alarm1 = spy(Alarm.builder().id(100L).isRead(false).build());
        Alarm alarm2 = spy(Alarm.builder().id(101L).isRead(false).build());

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(alarmRepository.findByMemberIdAndIsRead(memberId, false)).willReturn(List.of(alarm1, alarm2));

        // when
        AlarmReadAllDto.Response response = alarmService.modifyAllAlarmRead(memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(alarm1).readAlarm();
        verify(alarm2).readAlarm();
    }
}
