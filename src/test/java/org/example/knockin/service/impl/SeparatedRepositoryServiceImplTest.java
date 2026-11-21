package org.example.knockin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.example.knockin.alarm.service.impl.AlarmServiceImpl;
import org.example.knockin.chat.service.impl.ChatRoomMemberServiceImpl;
import org.example.knockin.alarm.service.impl.ChattingRequiredAlarmServiceImpl;
import org.example.knockin.chat.service.impl.ChattingRequiredServiceImpl;
import org.example.knockin.alarm.entity.AlarmType;
import org.example.knockin.board.entity.RoommateBoard;
import org.example.knockin.chat.entity.ChattingRequired;
import org.example.knockin.alarm.entity.ChattingRequiredAlarm;
import org.example.knockin.chat.entity.ChattingRequiredStatus;
import org.example.knockin.chat.entity.ChattingRoom;
import org.example.knockin.member.entity.BasicInformation;
import org.example.knockin.member.entity.Gender;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberInterest;
import org.example.knockin.mate.entity.RoommateMatchingRequired;
import org.example.knockin.alarm.entity.RoommateMatchingRequiredAlarm;
import org.example.knockin.mate.entity.RoommateRequiredStatus;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.chat.exception.ChattingErrorCode;
import org.example.knockin.global.exception.MemberErrorCode;
import org.example.knockin.chat.repository.ChatRoomMemberRepository;
import org.example.knockin.chat.repository.ChattingRequiredRepository;
import org.example.knockin.alarm.service.impl.RoommateMatchingRequiredAlarmServiceImpl;
import org.example.knockin.member.repository.BasicInformationFileRepository;
import org.example.knockin.member.repository.BasicInformationRepository;
import org.example.knockin.member.repository.MemberInterestRepository;
import org.example.knockin.member.service.impl.BasicInformationServiceImpl;
import org.example.knockin.member.service.impl.MemberInterestServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("분리된 저장소 서비스")
class SeparatedRepositoryServiceImplTest {

    @Mock
    private ChattingRequiredRepository chattingRequiredRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private MemberInterestRepository memberInterestRepository;

    @Mock
    private BasicInformationRepository basicInformationRepository;

    @Mock
    private AlarmServiceImpl alarmService;

    @Test
    @DisplayName("채팅 요청 저장 서비스는 대기 상태 요청을 저장한다")
    void chattingRequiredServiceSavesPendingRequest() {
        // Given
        ChattingRequiredServiceImpl service = new ChattingRequiredServiceImpl(chattingRequiredRepository);
        Member requester = Member.builder().id(1L).build();
        Member requestee = Member.builder().id(2L).build();
        RoommateBoard board = RoommateBoard.builder().id(10L).build();
        when(chattingRequiredRepository.save(any(ChattingRequired.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ChattingRequired saved = service.savePending(requester.getId(), requestee.getId(), board.getId());

        // Then
        assertThat(saved.getRequesterId()).isEqualTo(requester.getId());
        assertThat(saved.getRequesteeId()).isEqualTo(requestee.getId());
        assertThat(saved.getRoommateBoardId()).isEqualTo(board.getId());
        assertThat(saved.getStatus()).isEqualTo(ChattingRequiredStatus.PENDING);
    }

    @Test
    @DisplayName("채팅방 멤버 저장 서비스는 세 명 이상 저장을 거부한다")
    void chatRoomMemberServiceRejectsMoreThanTwoMembers() {
        // Given
        ChatRoomMemberServiceImpl service = new ChatRoomMemberServiceImpl(chatRoomMemberRepository);
        ChattingRoom chattingRoom = ChattingRoom.builder().id(10L).build();

        // When & Then
        assertThatThrownBy(() -> service.saveAll(chattingRoom, List.of(
                1L, 2L, 3L
        )))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ChattingErrorCode.ROOM_CAPACITY_EXCEEDED));
        verify(chatRoomMemberRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("채팅방 멤버 서비스는 활성 멤버가 아니면 구독을 거부한다")
    void chatRoomMemberServiceRejectsSubscriptionWhenMemberIsNotActive() {
        // Given
        ChatRoomMemberServiceImpl service = new ChatRoomMemberServiceImpl(chatRoomMemberRepository);
        when(chatRoomMemberRepository.existsActiveMember(10L, 1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> service.checkCanSubscribe(10L, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ChattingErrorCode.ROOM_ACCESS_DENIED));
    }

    @Test
    @DisplayName("매칭 관심 서비스는 기존 관심이 없으면 새 관심을 저장한다")
    void memberInterestServiceSavesWhenInterestMissing() {
        // Given
        MemberInterestServiceImpl service = new MemberInterestServiceImpl(memberInterestRepository);
        Member sender = Member.builder().id(1L).build();
        Member receiver = Member.builder().id(2L).build();
        when(memberInterestRepository.findBySenderIdAndReceiverIdForUpdate(sender.getId(), receiver.getId()))
                .thenReturn(Optional.empty());
        when(memberInterestRepository.save(any(MemberInterest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        MemberInterest saved = service.toggle(sender, receiver);

        // Then
        ArgumentCaptor<MemberInterest> captor = ArgumentCaptor.forClass(MemberInterest.class);
        verify(memberInterestRepository).save(captor.capture());
        assertThat(saved).isSameAs(captor.getValue());
        assertThat(saved.getSender()).isSameAs(sender);
        assertThat(saved.getReceiver()).isSameAs(receiver);
        assertThat(saved.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("매칭 관심 서비스는 기존 관심이 있으면 삭제 상태를 토글하고 새로 저장하지 않는다")
    void memberInterestServiceTogglesExistingInterestWithoutSavingNewOne() {
        // Given
        MemberInterestServiceImpl service = new MemberInterestServiceImpl(memberInterestRepository);
        Member sender = Member.builder().id(1L).build();
        Member receiver = Member.builder().id(2L).build();
        MemberInterest existing = MemberInterest.builder()
                .sender(sender)
                .receiver(receiver)
                .isDeleted(false)
                .build();
        when(memberInterestRepository.findBySenderIdAndReceiverIdForUpdate(sender.getId(), receiver.getId()))
                .thenReturn(Optional.of(existing));

        // When
        MemberInterest toggled = service.toggle(sender, receiver);

        // Then
        assertThat(toggled).isSameAs(existing);
        assertThat(toggled.getIsDeleted()).isTrue();
        verify(memberInterestRepository, never()).save(any(MemberInterest.class));
    }

    @Test
    @DisplayName("채팅 요청 알림 서비스는 행위자 이름으로 알림을 저장하고 클라이언트에 전송한다")
    void chattingRequiredAlarmServiceSavesAlarmAndSendsToClient() {
        // Given
        BasicInformationServiceImpl basicInformationService = new BasicInformationServiceImpl(basicInformationRepository, org.mockito.Mockito.mock(
                BasicInformationFileRepository.class));
        ChattingRequiredAlarmServiceImpl service = new ChattingRequiredAlarmServiceImpl(
                basicInformationService,
                alarmService
        );
        ReflectionTestUtils.setField(service, "requestAlarmExpireDays", 7);
        Member receiver = Member.builder().id(2L).build();
        Member actor = Member.builder().id(1L).build();
        ChattingRequired required = ChattingRequired.builder()
                .id(100L)
                .requesterId(actor.getId())
                .requesteeId(receiver.getId())
                .status(ChattingRequiredStatus.PENDING)
                .build();

        when(basicInformationRepository.findLatestBasicInformation(actor))
                .thenReturn(Optional.of(basicInformation(actor, "김중민")));

        // When
        service.send(receiver, actor, required.getId(), "%s님이 매칭을 요청했어요");

        // Then
        ArgumentCaptor<ChattingRequiredAlarm> captor = ArgumentCaptor.forClass(ChattingRequiredAlarm.class);
        verify(alarmService).sendToClient(eq(receiver.getId()), eq(AlarmType.CHATTING_REQUIRED.name()), captor.capture());
        ChattingRequiredAlarm alarm = captor.getValue();
        assertThat(alarm.getMemberId()).isEqualTo(receiver.getId());
        assertThat(alarm.getTitle()).isEqualTo("김중민님이 매칭을 요청했어요");
        assertThat(alarm.getContents()).isEqualTo("김중민님이 매칭을 요청했어요");
        assertThat(alarm.getType()).isEqualTo(AlarmType.CHATTING_REQUIRED);
        assertThat(alarm.getChattingRequiredId()).isEqualTo(required.getId());
        assertThat(alarm.getExpiredAt()).isAfter(LocalDateTime.now().plusDays(6));
    }

    @Test
    @DisplayName("룸메이트 확정 요청 알림 서비스는 대기 상태 문구로 알림을 저장하고 클라이언트에 전송한다")
    void roommateMatchingRequiredAlarmServiceSavesPendingAlarmAndSendsToClient() {
        // Given
        RoommateMatchingRequiredAlarmServiceImpl service = new RoommateMatchingRequiredAlarmServiceImpl(
                alarmService
        );
        ReflectionTestUtils.setField(service, "requestAlarmExpireDays", 7);
        Member receiver = Member.builder().id(2L).build();
        Member sender = Member.builder().id(1L).build();
        RoommateMatchingRequired required = RoommateMatchingRequired.builder()
                .requester(sender)
                .requestee(receiver)
                .chattingRoomId(10L)
                .status(RoommateRequiredStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(required, "id", 100L);

        // When
        service.send(
                receiver,
                "이수현님의 매칭 요청",
                "이수현님이 매칭을 요청했어요.",
                required.getId()
        );

        // Then
        ArgumentCaptor<RoommateMatchingRequiredAlarm> captor = ArgumentCaptor.forClass(RoommateMatchingRequiredAlarm.class);
        verify(alarmService).sendToClient(eq(receiver.getId()), eq(AlarmType.ROOM_MATCHING.name()), captor.capture());
        RoommateMatchingRequiredAlarm alarm = captor.getValue();
        assertThat(alarm.getMemberId()).isEqualTo(receiver.getId());
        assertThat(alarm.getTitle()).isEqualTo("이수현님의 매칭 요청");
        assertThat(alarm.getContents()).isEqualTo("이수현님이 매칭을 요청했어요.");
        assertThat(alarm.getIsRead()).isFalse();
        assertThat(alarm.getType()).isEqualTo(AlarmType.ROOM_MATCHING);
        assertThat(alarm.getRoommateMatchingRequiredId()).isEqualTo(required.getId());
        assertThat(alarm.getExpiredAt()).isAfter(LocalDateTime.now().plusDays(6));
    }

    @Test
    @DisplayName("알림 서비스는 행위자의 기본 정보가 없으면 알림을 저장하지 않는다")
    void alarmServiceDoesNotSaveWhenActorBasicInformationIsMissing() {
        // Given
        BasicInformationServiceImpl basicInformationService = new BasicInformationServiceImpl(basicInformationRepository, org.mockito.Mockito.mock(
                BasicInformationFileRepository.class));
        ChattingRequiredAlarmServiceImpl service = new ChattingRequiredAlarmServiceImpl(
                basicInformationService,
                alarmService
        );
        ReflectionTestUtils.setField(service, "requestAlarmExpireDays", 7);
        Member receiver = Member.builder().id(2L).build();
        Member actor = Member.builder().id(1L).build();
        ChattingRequired required = ChattingRequired.builder()
                .requesterId(actor.getId())
                .requesteeId(receiver.getId())
                .status(ChattingRequiredStatus.PENDING)
                .build();
        when(basicInformationRepository.findLatestBasicInformation(actor)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.send(receiver, actor, required.getId(), "%s님이 매칭을 요청했어요"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.BASIC_INFO_NOT_FOUND));
        verifyNoInteractions(alarmService);
    }

    private BasicInformation basicInformation(Member member, String name) {
        return BasicInformation.builder()
                .member(member)
                .name(name)
                .birth(LocalDate.of(1998, 1, 1))
                .gender(Gender.MALE)
                .email(name + "@example.com")
                .build();
    }
}
