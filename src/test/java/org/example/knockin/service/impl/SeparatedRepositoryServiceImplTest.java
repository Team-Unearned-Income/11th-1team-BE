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
import org.example.knockin.mate.dto.CalendarDto;
import org.example.knockin.mate.dto.RepeatCalendarDto;
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
import org.example.knockin.mate.entity.MyRoommate;
import org.example.knockin.mate.entity.RepeatRoommateCalendar;
import org.example.knockin.mate.entity.RepeatType;
import org.example.knockin.mate.entity.RoommateCalendar;
import org.example.knockin.mate.entity.RoommateCalendarCategory;
import org.example.knockin.mate.entity.RoommateMatchingRequired;
import org.example.knockin.alarm.entity.RoommateMatchingRequiredAlarm;
import org.example.knockin.mate.entity.RoommateRequiredStatus;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.ChattingErrorCode;
import org.example.knockin.global.exception.MemberErrorCode;
import org.example.knockin.global.exception.MyRoommateErrorCode;
import org.example.knockin.global.exception.RequiredErrorCode;
import org.example.knockin.chat.repository.ChatRoomMemberRepository;
import org.example.knockin.chat.repository.ChattingRequiredRepository;
import org.example.knockin.alarm.service.impl.RoommateMatchingRequiredAlarmServiceImpl;
import org.example.knockin.mate.service.impl.RoommateMatchingRequiredServiceImpl;
import org.example.knockin.member.repository.BasicInformationFileRepository;
import org.example.knockin.member.repository.BasicInformationRepository;
import org.example.knockin.member.repository.MemberInterestRepository;
import org.example.knockin.mate.repository.RepeatRoommateCalendarRepository;
import org.example.knockin.mate.repository.RoommateCalendarRepository;
import org.example.knockin.mate.repository.RoommateMatchingRequiredRepository;
import org.example.knockin.mate.service.impl.RepeatRoommateCalendarServiceImpl;
import org.example.knockin.mate.service.impl.RoommateCalendarServiceImpl;
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
    private RoommateCalendarRepository roommateCalendarRepository;

    @Mock
    private RepeatRoommateCalendarRepository repeatRoommateCalendarRepository;

    @Mock
    private ChattingRequiredRepository chattingRequiredRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private RoommateMatchingRequiredRepository roommateMatchingRequiredRepository;

    @Mock
    private MemberInterestRepository memberInterestRepository;

    @Mock
    private BasicInformationRepository basicInformationRepository;

    @Mock
    private AlarmServiceImpl alarmService;

    @Test
    @DisplayName("캘린더 저장 서비스는 요청 값으로 룸메이트 캘린더를 저장한다")
    void roommateCalendarServiceSavesCalendarValues() {
        // Given
        RoommateCalendarServiceImpl service = new RoommateCalendarServiceImpl(roommateCalendarRepository);
        MyRoommate myRoommate = MyRoommate.builder().id(10L).build();
        Member owner = Member.builder().id(1L).build();
        RoommateCalendarCategory category = RoommateCalendarCategory.builder().id(20L).name("청소").build();
        CalendarDto.CalendarInfoDto dto = new CalendarDto.CalendarInfoDto();
        dto.setTitle("거실 청소");
        dto.setContents("저녁 전까지");
        dto.setStartDate(LocalDateTime.of(2026, 7, 10, 10, 0));
        dto.setEndDate(LocalDateTime.of(2026, 7, 10, 11, 0));
        when(roommateCalendarRepository.save(any(RoommateCalendar.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RoommateCalendar saved = service.save(myRoommate, owner, category, dto);

        // Then
        assertThat(saved.getMyRoommate()).isSameAs(myRoommate);
        assertThat(saved.getMember()).isSameAs(owner);
        assertThat(saved.getRoommateCalendarCategory()).isSameAs(category);
        assertThat(saved.getTitle()).isEqualTo("거실 청소");
        assertThat(saved.getContents()).isEqualTo("저녁 전까지");
        verify(roommateCalendarRepository).save(saved);
    }

    @Test
    @DisplayName("캘린더 저장 서비스는 캘린더가 없으면 캘린더 없음 예외를 던진다")
    void roommateCalendarServiceThrowsWhenCalendarMissing() {
        // Given
        RoommateCalendarServiceImpl service = new RoommateCalendarServiceImpl(roommateCalendarRepository);
        when(roommateCalendarRepository.findById(100L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.findByIdOrThrow(100L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MyRoommateErrorCode.CALENDER_NOT_FOUND));
    }

    @Test
    @DisplayName("반복 캘린더 서비스는 반복 정보의 종료일과 반복 타입을 저장한다")
    void repeatRoommateCalendarServiceSavesRepeatInfo() {
        // Given
        RepeatRoommateCalendarServiceImpl service = new RepeatRoommateCalendarServiceImpl(repeatRoommateCalendarRepository);
        RoommateCalendar calendar = RoommateCalendar.builder().id(100L).build();
        RepeatCalendarDto.RepeatCalendarInfo repeatInfo = new RepeatCalendarDto.RepeatCalendarInfo();
        repeatInfo.setEndDate(LocalDateTime.of(2026, 8, 1, 10, 0));
        repeatInfo.setRepeatType(RepeatType.WEEKLY);
        when(repeatRoommateCalendarRepository.save(any(RepeatRoommateCalendar.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RepeatRoommateCalendar saved = service.save(calendar, repeatInfo);

        // Then
        assertThat(saved.getRoommateCalendar()).isSameAs(calendar);
        assertThat(saved.getEndDate()).isEqualTo(LocalDateTime.of(2026, 8, 1, 10, 0));
        assertThat(saved.getRepeatType()).isEqualTo(RepeatType.WEEKLY);
    }

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
        ChattingRequired saved = service.savePending(requester, requestee, board);

        // Then
        assertThat(saved.getRequester()).isSameAs(requester);
        assertThat(saved.getRequestee()).isSameAs(requestee);
        assertThat(saved.getRoommateBoard()).isSameAs(board);
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
                Member.builder().id(1L).build(),
                Member.builder().id(2L).build(),
                Member.builder().id(3L).build()
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
    @DisplayName("룸메이트 확정 요청 저장 서비스는 대기 상태 요청을 저장한다")
    void roommateMatchingRequiredServiceSavesPendingRequest() {
        // Given
        RoommateMatchingRequiredServiceImpl service = new RoommateMatchingRequiredServiceImpl(roommateMatchingRequiredRepository);
        Member requester = Member.builder().id(1L).build();
        Member requestee = Member.builder().id(2L).build();
        ChattingRoom chattingRoom = ChattingRoom.builder().id(10L).build();
        when(roommateMatchingRequiredRepository.save(any(RoommateMatchingRequired.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RoommateMatchingRequired saved = service.savePending(requester, requestee, chattingRoom);

        // Then
        assertThat(saved.getRequester()).isSameAs(requester);
        assertThat(saved.getRequestee()).isSameAs(requestee);
        assertThat(saved.getChattingRoom()).isSameAs(chattingRoom);
        assertThat(saved.getStatus()).isEqualTo(RoommateRequiredStatus.PENDING);
    }

    @Test
    @DisplayName("룸메이트 확정 요청 저장 서비스는 요청이 없으면 조회 실패 예외를 던진다")
    void roommateMatchingRequiredServiceThrowsWhenRequestMissing() {
        // Given
        RoommateMatchingRequiredServiceImpl service = new RoommateMatchingRequiredServiceImpl(roommateMatchingRequiredRepository);
        when(roommateMatchingRequiredRepository.findById(1000L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.findByIdOrThrow(1000L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(RequiredErrorCode.ROOMMATE_NOT_FOUND));
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
                .requester(actor)
                .requestee(receiver)
                .status(ChattingRequiredStatus.PENDING)
                .build();

        when(basicInformationRepository.findLatestBasicInformation(actor))
                .thenReturn(Optional.of(basicInformation(actor, "김중민")));

        // When
        service.send(receiver, actor, required, "%s님이 매칭을 요청했어요");

        // Then
        ArgumentCaptor<ChattingRequiredAlarm> captor = ArgumentCaptor.forClass(ChattingRequiredAlarm.class);
        verify(alarmService).sendToClient(eq(receiver.getId()), eq(AlarmType.CHATTING_REQUIRED.name()), captor.capture());
        ChattingRequiredAlarm alarm = captor.getValue();
        assertThat(alarm.getMember()).isSameAs(receiver);
        assertThat(alarm.getTitle()).isEqualTo("김중민님이 매칭을 요청했어요");
        assertThat(alarm.getContents()).isEqualTo("김중민님이 매칭을 요청했어요");
        assertThat(alarm.getType()).isEqualTo(AlarmType.CHATTING_REQUIRED);
        assertThat(alarm.getChattingRequired()).isSameAs(required);
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
                .chattingRoom(ChattingRoom.builder().id(10L).build())
                .status(RoommateRequiredStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(required, "id", 100L);

        // When
        service.send(
                receiver,
                "이수현님의 매칭 요청",
                "이수현님이 매칭을 요청했어요.",
                required
        );

        // Then
        ArgumentCaptor<RoommateMatchingRequiredAlarm> captor = ArgumentCaptor.forClass(RoommateMatchingRequiredAlarm.class);
        verify(alarmService).sendToClient(eq(receiver.getId()), eq(AlarmType.ROOM_MATCHING.name()), captor.capture());
        RoommateMatchingRequiredAlarm alarm = captor.getValue();
        assertThat(alarm.getMember()).isSameAs(receiver);
        assertThat(alarm.getTitle()).isEqualTo("이수현님의 매칭 요청");
        assertThat(alarm.getContents()).isEqualTo("이수현님이 매칭을 요청했어요.");
        assertThat(alarm.getIsRead()).isFalse();
        assertThat(alarm.getType()).isEqualTo(AlarmType.ROOM_MATCHING);
        assertThat(alarm.getRoommateMatchingRequired()).isSameAs(required);
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
                .requester(actor)
                .requestee(receiver)
                .status(ChattingRequiredStatus.PENDING)
                .build();
        when(basicInformationRepository.findLatestBasicInformation(actor)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.send(receiver, actor, required, "%s님이 매칭을 요청했어요"))
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
