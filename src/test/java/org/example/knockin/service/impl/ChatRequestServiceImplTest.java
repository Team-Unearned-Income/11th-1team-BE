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
import java.util.Map;
import java.util.Optional;
import org.example.knockin.alarm.service.impl.AlarmServiceImpl;
import org.example.knockin.chat.dto.ChatRequestDetailDto;
import org.example.knockin.chat.dto.ChatRequestListDto;
import org.example.knockin.chat.dto.ChatRequestDto;
import org.example.knockin.alarm.entity.AlarmType;
import org.example.knockin.board.entity.RoommateBoard;
import org.example.knockin.chat.entity.ChattingRequired;
import org.example.knockin.alarm.entity.ChattingRequiredAlarm;
import org.example.knockin.chat.entity.ChattingRequiredStatus;
import org.example.knockin.chat.entity.ChattingRoom;
import org.example.knockin.chat.service.impl.ChatRequestServiceImpl;
import org.example.knockin.alarm.service.impl.ChattingRequiredAlarmServiceImpl;
import org.example.knockin.chat.service.impl.ChattingRequiredServiceImpl;
import org.example.knockin.life.service.impl.MemberLifePatternService;
import org.example.knockin.life.entity.LifePatternType;
import org.example.knockin.member.entity.BasicInformation;
import org.example.knockin.member.entity.Gender;
import org.example.knockin.member.entity.Member;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.CommonErrorCode;
import org.example.knockin.global.exception.MemberErrorCode;
import org.example.knockin.global.exception.RequiredErrorCode;
import org.example.knockin.global.exception.RoommateBoardErrorCode;
import org.example.knockin.global.util.DateUtils;
import org.example.knockin.board.repository.RoommateBoardRepository;
import org.example.knockin.chat.repository.ChatRoomMemberRepository;
import org.example.knockin.alarm.repository.ChattingRequiredAlarmRepository;
import org.example.knockin.chat.repository.ChattingRequiredRepository;
import org.example.knockin.chat.repository.ChattingRoomRepository;
import org.example.knockin.chat.repository.row.ChatRequestListRow;
import org.example.knockin.life.repository.MemberLifePatternRepository;
import org.example.knockin.life.repository.row.MatchingLifestyleRow;
import org.example.knockin.member.repository.BasicInformationFileRepository;
import org.example.knockin.member.repository.BasicInformationRepository;
import org.example.knockin.member.repository.MemberRepository;
import org.example.knockin.member.repository.row.ChattingRoomBasicInfoRow;
import org.example.knockin.member.service.impl.BasicInformationServiceImpl;
import org.example.knockin.util.service.RoommateScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("채팅 요청 서비스")
class ChatRequestServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ChattingRequiredRepository chattingRequiredRepository;

    @Mock
    private RoommateBoardRepository roommateBoardRepository;

    @Mock
    private ChattingRoomRepository chattingRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private AlarmServiceImpl alarmService;

    @Mock
    private ChattingRequiredAlarmRepository chattingRequiredAlarmRepository;

    @Mock
    private BasicInformationRepository basicInformationRepository;

    @Mock
    private MemberLifePatternRepository memberLifePatternRepository;

    @Mock
    private RoommateScoreService roommateScoreService;

    @InjectMocks
    private ChatRequestServiceImpl chatRequestService;

    @BeforeEach
    void setUp() {
        BasicInformationServiceImpl basicInformationService = new BasicInformationServiceImpl(basicInformationRepository, org.mockito.Mockito.mock(
                BasicInformationFileRepository.class));
        MemberLifePatternService memberLifePatternService = new MemberLifePatternService(memberLifePatternRepository, null, null, null);
        ChattingRequiredAlarmServiceImpl chattingRequiredAlarmService =
                new ChattingRequiredAlarmServiceImpl(
                        basicInformationService,
                        alarmService);
        ReflectionTestUtils.setField(chattingRequiredAlarmService, "requestAlarmExpireDays", 7);

        chatRequestService = new ChatRequestServiceImpl(
                memberRepository,
                new ChattingRequiredServiceImpl(chattingRequiredRepository),
                roommateBoardRepository,
                chattingRequiredAlarmService,
                basicInformationService,
                memberLifePatternService,
                roommateScoreService
        );
    }

    @Test
    @DisplayName("대기 중인 채팅 요청 목록을 조회하면 요청자 정보와 임시 점수를 반환한다")
    void getPendingChatRequestListReturnsRequesterInfoWithTemporaryScore() {
        // Given
        Long memberId = 1L;
        Member requestee = Member.builder().id(memberId).isDelete(false).build();
        LocalDate requesterBirth = LocalDate.of(2000, 1, 1);
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 23, 10, 0);
        ChatRequestListRow row = new ChatRequestListRow(
                1000L,
                ChattingRequiredStatus.PENDING,
                2L,
                "요청자",
                requesterBirth,
                Gender.FEMALE,
                createdAt
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(requestee));
        when(chattingRequiredRepository.findAllPendingByRequesteeId(requestee.getId())).thenReturn(List.of(row));
        when(roommateScoreService.calculateSimpleScores(memberId, List.of(row.memberId()))).thenReturn(Map.of(row.memberId(), 100));

        // When
        List<ChatRequestListDto.Response> responses = chatRequestService.getPendingChatRequestList(memberId);

        // Then
        assertThat(responses).hasSize(1);
        ChatRequestListDto.Response response = responses.getFirst();
        assertThat(response.getRequiredId()).isEqualTo(1000L);
        assertThat(response.getStatus()).isEqualTo(ChattingRequiredStatus.PENDING);
        assertThat(response.getMemberId()).isEqualTo(2L);
        assertThat(response.getMemberName()).isEqualTo("요청자");
        assertThat(response.getMemberAge()).isEqualTo(DateUtils.calculateAge(requesterBirth));
        assertThat(response.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(response.getScore()).isEqualTo(100);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        verify(chattingRequiredRepository).findAllPendingByRequesteeId(requestee.getId());
        verifyNoInteractions(roommateBoardRepository, chattingRoomRepository, chatRoomMemberRepository,
                basicInformationRepository, chattingRequiredAlarmRepository, alarmService);
    }

    @Test
    @DisplayName("대기 중인 채팅 요청이 없으면 빈 목록을 반환한다")
    void getPendingChatRequestListReturnsEmptyList() {
        // Given
        Long memberId = 1L;
        Member requestee = Member.builder().id(memberId).isDelete(false).build();
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(requestee));
        when(chattingRequiredRepository.findAllPendingByRequesteeId(requestee.getId())).thenReturn(List.of());

        // When
        List<ChatRequestListDto.Response> responses = chatRequestService.getPendingChatRequestList(memberId);

        // Then
        assertThat(responses).isEmpty();
        verify(chattingRequiredRepository).findAllPendingByRequesteeId(requestee.getId());
        verifyNoInteractions(roommateBoardRepository, chattingRoomRepository, chatRoomMemberRepository,
                basicInformationRepository, chattingRequiredAlarmRepository, alarmService);
    }

    @Test
    @DisplayName("채팅 요청 목록 조회 회원이 없으면 회원 없음 예외를 던지고 요청 목록을 조회하지 않는다")
    void getPendingChatRequestListThrowsWhenMemberDoesNotExist() {
        // Given
        Long memberId = 1L;
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatRequestService.getPendingChatRequestList(memberId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        verifyNoInteractions(chattingRequiredRepository, roommateBoardRepository, chattingRoomRepository,
                chatRoomMemberRepository, basicInformationRepository, chattingRequiredAlarmRepository, alarmService);
    }

    @Test
    @DisplayName("피요청자가 채팅 요청 상세를 조회하면 본인과 요청자 정보를 구분해 반환한다")
    void getChatRequestDetailReturnsRequesteePerspective() {
        // Given
        Long requestId = 1000L;
        Long requesterId = 1L;
        Long requesteeId = 2L;
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 25, 9, 0);
        Member requester = Member.builder().id(requesterId).isDelete(false).build();
        Member requestee = Member.builder().id(requesteeId).isDelete(false).build();
        ChattingRequired chattingRequired = chatRequest(requestId, requester, requestee, ChattingRequiredStatus.PENDING, createdAt);
        LocalDate requesteeBirth = LocalDate.of(2001, 1, 1);
        LocalDate requesterBirth = LocalDate.of(2000, 2, 2);

        when(chattingRequiredRepository.findById(requestId)).thenReturn(Optional.of(chattingRequired));
        when(basicInformationRepository.findChattingRoomBasicInfoRows(List.of(requesteeId, requesterId)))
                .thenReturn(List.of(
                        basicInfoRow(requesteeId, "피요청자", requesteeBirth, Gender.FEMALE, "requestee.jpg"),
                        basicInfoRow(requesterId, "요청자", requesterBirth, Gender.MALE, "requester.jpg")
                ));
        when(memberLifePatternRepository.findAllLifestyleByMemberIdIn(List.of(requesteeId, requesterId)))
                .thenReturn(List.of(
                        lifestyleRow(requesteeId, 11L, "청결", "4", "깔끔한 편", LifePatternType.SCALE),
                        lifestyleRow(requesterId, 12L, "흡연", "비흡연", "흡연하지 않음", LifePatternType.SINGLE_CHOICE)
                ));
        when(roommateScoreService.calculateSimpleScore(requesteeId, requesterId)).thenReturn(100);

        // When
        ChatRequestDetailDto.Response response = chatRequestService.getChatRequestDetail(requesteeId, requestId);

        // Then
        assertThat(response.getRequiredId()).isEqualTo(requestId);
        assertThat(response.getStatus()).isEqualTo(ChattingRequiredStatus.PENDING);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getScore()).isEqualTo(100);
        assertThat(response.getIsRequester()).isFalse();

        assertThat(response.getMe().getMemberId()).isEqualTo(requesteeId);
        assertThat(response.getMe().getMemberName()).isEqualTo("피요청자");
        assertThat(response.getMe().getMemberAge()).isEqualTo(DateUtils.calculateAge(requesteeBirth));
        assertThat(response.getMe().getGender()).isEqualTo(Gender.FEMALE);
        assertThat(response.getMe().getMemberProfileImageUrl()).isEqualTo("requestee.jpg");
        assertThat(response.getMe().getLifeStyles()).hasSize(1);
        assertThat(response.getMe().getLifeStyles().getFirst().getLifestyleId()).isEqualTo(11L);
        assertThat(response.getMe().getLifeStyles().getFirst().getName()).isEqualTo("청결");

        assertThat(response.getOpponent().getMemberId()).isEqualTo(requesterId);
        assertThat(response.getOpponent().getMemberName()).isEqualTo("요청자");
        assertThat(response.getOpponent().getMemberAge()).isEqualTo(DateUtils.calculateAge(requesterBirth));
        assertThat(response.getOpponent().getGender()).isEqualTo(Gender.MALE);
        assertThat(response.getOpponent().getMemberProfileImageUrl()).isEqualTo("requester.jpg");
        assertThat(response.getOpponent().getLifeStyles()).hasSize(1);
        assertThat(response.getOpponent().getLifeStyles().getFirst().getLifestyleId()).isEqualTo(12L);
        assertThat(response.getOpponent().getLifeStyles().getFirst().getType()).isEqualTo(LifePatternType.SINGLE_CHOICE);
    }

    @Test
    @DisplayName("요청자가 채팅 요청 상세를 조회하면 요청자 여부를 참으로 반환하고 라이프스타일이 없으면 빈 목록을 반환한다")
    void getChatRequestDetailReturnsRequesterPerspectiveWithEmptyLifestyle() {
        // Given
        Long requestId = 1000L;
        Long requesterId = 1L;
        Long requesteeId = 2L;
        Member requester = Member.builder().id(requesterId).isDelete(false).build();
        Member requestee = Member.builder().id(requesteeId).isDelete(false).build();
        ChattingRequired chattingRequired = chatRequest(requestId, requester, requestee, ChattingRequiredStatus.ACCEPTED,
                LocalDateTime.of(2026, 6, 25, 9, 30));

        when(chattingRequiredRepository.findById(requestId)).thenReturn(Optional.of(chattingRequired));
        when(basicInformationRepository.findChattingRoomBasicInfoRows(List.of(requesterId, requesteeId)))
                .thenReturn(List.of(
                        basicInfoRow(requesterId, "요청자", LocalDate.of(2000, 1, 1), Gender.MALE, null),
                        basicInfoRow(requesteeId, "피요청자", LocalDate.of(2001, 1, 1), Gender.FEMALE, null)
                ));
        when(memberLifePatternRepository.findAllLifestyleByMemberIdIn(List.of(requesterId, requesteeId)))
                .thenReturn(List.of());
        when(roommateScoreService.calculateSimpleScore(requesterId, requesteeId)).thenReturn(100);

        // When
        ChatRequestDetailDto.Response response = chatRequestService.getChatRequestDetail(requesterId, requestId);

        // Then
        assertThat(response.getIsRequester()).isTrue();
        assertThat(response.getMe().getMemberId()).isEqualTo(requesterId);
        assertThat(response.getOpponent().getMemberId()).isEqualTo(requesteeId);
        assertThat(response.getMe().getLifeStyles()).isEmpty();
        assertThat(response.getOpponent().getLifeStyles()).isEmpty();
    }

    @Test
    @DisplayName("채팅 요청 당사자가 아닌 회원이 상세를 조회하면 권한 없음 예외를 던진다")
    void getChatRequestDetailRejectsUnrelatedMember() {
        // Given
        Long requestId = 1000L;
        Member requester = Member.builder().id(1L).isDelete(false).build();
        Member requestee = Member.builder().id(2L).isDelete(false).build();
        ChattingRequired chattingRequired = chatRequest(requester, requestee, ChattingRequiredStatus.PENDING);
        when(chattingRequiredRepository.findById(requestId)).thenReturn(Optional.of(chattingRequired));

        // When & Then
        assertThatThrownBy(() -> chatRequestService.getChatRequestDetail(3L, requestId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(RequiredErrorCode.CHATTING_ACCESS_DENIED));
        verifyNoInteractions(basicInformationRepository, memberLifePatternRepository);
    }

    @Test
    @DisplayName("존재하지 않는 채팅 요청 상세를 조회하면 조회 실패 예외를 던진다")
    void getChatRequestDetailRejectsMissingRequest() {
        // Given
        Long requestId = 1000L;
        when(chattingRequiredRepository.findById(requestId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatRequestService.getChatRequestDetail(1L, requestId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(RequiredErrorCode.CHATTING_NOT_FOUND));
        verifyNoInteractions(basicInformationRepository, memberLifePatternRepository);
    }

    @Test
    @DisplayName("채팅 요청 상세 조회에 필요한 기본 정보가 없으면 기본 정보 없음 예외를 던진다")
    void getChatRequestDetailRejectsMissingBasicInformation() {
        // Given
        Long requestId = 1000L;
        Long requesterId = 1L;
        Long requesteeId = 2L;
        Member requester = Member.builder().id(requesterId).isDelete(false).build();
        Member requestee = Member.builder().id(requesteeId).isDelete(false).build();
        ChattingRequired chattingRequired = chatRequest(requestId, requester, requestee, ChattingRequiredStatus.PENDING,
                LocalDateTime.of(2026, 6, 25, 10, 0));
        when(chattingRequiredRepository.findById(requestId)).thenReturn(Optional.of(chattingRequired));
        when(basicInformationRepository.findChattingRoomBasicInfoRows(List.of(requesteeId, requesterId)))
                .thenReturn(List.of(basicInfoRow(requesteeId, "피요청자", LocalDate.of(2001, 1, 1), Gender.FEMALE, null)));
        when(memberLifePatternRepository.findAllLifestyleByMemberIdIn(List.of(requesteeId, requesterId))).thenReturn(List.of());

        // When & Then
        assertThatThrownBy(() -> chatRequestService.getChatRequestDetail(requesteeId, requestId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.BASIC_INFO_NOT_FOUND));
    }

    @Test
    @DisplayName("중복 요청이 아니면 대기 중인 채팅 요청을 저장하고 채팅방은 생성하지 않는다")
    void saveChatRequestCreatesPendingRequestWithoutChatRoom() {
        // Given
        Long requesterId = 1L;
        Long requesteeId = 2L;
        Long boardId = 10L;
        Member requester = Member.builder().id(requesterId).isDelete(false).build();
        Member requestee = Member.builder().id(requesteeId).isDelete(false).build();
        RoommateBoard roommateBoard = RoommateBoard.builder().id(boardId).build();
        ChatRequestDto.Request request = chatRequest(requesteeId, boardId);

        when(memberRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(memberRepository.findById(requesteeId)).thenReturn(Optional.of(requestee));
        when(chattingRequiredRepository.findLatest(requester.getId(), requestee.getId())).thenReturn(Optional.empty());
        when(roommateBoardRepository.findById(boardId)).thenReturn(Optional.of(roommateBoard));
        when(chattingRequiredRepository.save(any(ChattingRequired.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(basicInformationRepository.findLatestBasicInformation(requester))
                .thenReturn(Optional.of(basicInformation(requester, "김중민")));

        // When
        ChatRequestDto.Response response = chatRequestService.saveChatRequest(requesterId, request);

        // Then
        ArgumentCaptor<ChattingRequired> requiredCaptor = ArgumentCaptor.forClass(ChattingRequired.class);
        verify(chattingRequiredRepository).save(requiredCaptor.capture());
        ChattingRequired chattingRequired = requiredCaptor.getValue();
        assertThat(chattingRequired.getRequesterId()).isEqualTo(requester.getId());
        assertThat(chattingRequired.getRequesteeId()).isEqualTo(requestee.getId());
        assertThat(chattingRequired.getRoommateBoardId()).isEqualTo(roommateBoard.getId());
        assertThat(chattingRequired.getStatus()).isEqualTo(ChattingRequiredStatus.PENDING);
        assertChattingRequiredAlarm(requestee, "김중민님이 매칭을 요청했어요", chattingRequired);
        verify(alarmService).sendToClient(eq(requesteeId), eq(AlarmType.CHATTING_REQUIRED.name()), any(ChattingRequiredAlarm.class));
        verify(chattingRoomRepository, never()).save(any(ChattingRoom.class));
        verifyNoInteractions(chatRoomMemberRepository);
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("게시글이 없는 채팅 요청이면 게시글 조회 없이 대기 중인 채팅 요청을 저장한다")
    void saveChatRequestCreatesPendingRequestWithoutRoommateBoard() {
        // Given
        Long requesterId = 1L;
        Long requesteeId = 2L;
        Member requester = Member.builder().id(requesterId).isDelete(false).build();
        Member requestee = Member.builder().id(requesteeId).isDelete(false).build();
        ChatRequestDto.Request request = chatRequest(requesteeId, null);

        when(memberRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(memberRepository.findById(requesteeId)).thenReturn(Optional.of(requestee));
        when(chattingRequiredRepository.findLatest(requester.getId(), requestee.getId())).thenReturn(Optional.empty());
        when(chattingRequiredRepository.save(any(ChattingRequired.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(basicInformationRepository.findLatestBasicInformation(requester))
                .thenReturn(Optional.of(basicInformation(requester, "김중민")));

        // When
        ChatRequestDto.Response response = chatRequestService.saveChatRequest(requesterId, request);

        // Then
        ArgumentCaptor<ChattingRequired> requiredCaptor = ArgumentCaptor.forClass(ChattingRequired.class);
        verify(chattingRequiredRepository).save(requiredCaptor.capture());
        ChattingRequired chattingRequired = requiredCaptor.getValue();
        assertThat(chattingRequired.getRequesterId()).isEqualTo(requester.getId());
        assertThat(chattingRequired.getRequesteeId()).isEqualTo(requestee.getId());
        assertThat(chattingRequired.getRoommateBoardId()).isNull();
        assertThat(chattingRequired.getStatus()).isEqualTo(ChattingRequiredStatus.PENDING);
        assertChattingRequiredAlarm(requestee, "김중민님이 매칭을 요청했어요", chattingRequired);
        verify(alarmService).sendToClient(eq(requesteeId), eq(AlarmType.CHATTING_REQUIRED.name()), any(ChattingRequiredAlarm.class));
        verifyNoInteractions(roommateBoardRepository);
        verify(chattingRoomRepository, never()).save(any(ChattingRoom.class));
        verifyNoInteractions(chatRoomMemberRepository);
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("요청자 식별자가 없으면 잘못된 요청 예외를 던지고 회원을 조회하지 않는다")
    void saveChatRequestThrowsWhenRequesterIdIsNull() {
        // Given
        ChatRequestDto.Request request = chatRequest(2L, 10L);

        // When & Then
        assertThatThrownBy(() -> chatRequestService.saveChatRequest(null, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.BAD_REQUEST));
        verifyNoInteractions(memberRepository, chattingRequiredRepository, roommateBoardRepository, chattingRoomRepository,
                chatRoomMemberRepository, basicInformationRepository, chattingRequiredAlarmRepository, alarmService);
    }

    @Test
    @DisplayName("요청 본문이 없으면 잘못된 요청 예외를 던지고 회원을 조회하지 않는다")
    void saveChatRequestThrowsWhenRequestBodyIsNull() {
        // When & Then
        assertThatThrownBy(() -> chatRequestService.saveChatRequest(1L, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.BAD_REQUEST));
        verifyNoInteractions(memberRepository, chattingRequiredRepository, roommateBoardRepository, chattingRoomRepository,
                chatRoomMemberRepository, basicInformationRepository, chattingRequiredAlarmRepository, alarmService);
    }

    @Test
    @DisplayName("피요청자 식별자가 없으면 잘못된 요청 예외를 던지고 회원을 조회하지 않는다")
    void saveChatRequestThrowsWhenRequesteeIdIsNull() {
        // Given
        ChatRequestDto.Request request = chatRequest(null, 10L);

        // When & Then
        assertThatThrownBy(() -> chatRequestService.saveChatRequest(1L, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.BAD_REQUEST));
        verifyNoInteractions(memberRepository, chattingRequiredRepository, roommateBoardRepository, chattingRoomRepository,
                chatRoomMemberRepository, basicInformationRepository, chattingRequiredAlarmRepository, alarmService);
    }

    @Test
    @DisplayName("요청자와 피요청자가 같으면 잘못된 요청 예외를 던지고 회원을 조회하지 않는다")
    void saveChatRequestThrowsWhenRequesterAndRequesteeAreSame() {
        // Given
        Long requesterId = 1L;
        ChatRequestDto.Request request = chatRequest(requesterId, 10L);

        // When & Then
        assertThatThrownBy(() -> chatRequestService.saveChatRequest(requesterId, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.BAD_REQUEST));
        verifyNoInteractions(memberRepository, chattingRequiredRepository, roommateBoardRepository, chattingRoomRepository,
                chatRoomMemberRepository, basicInformationRepository, chattingRequiredAlarmRepository, alarmService);
    }

    @Test
    @DisplayName("요청자가 없으면 회원 없음 예외를 던지고 피요청자와 채팅 요청을 조회하지 않는다")
    void saveChatRequestThrowsWhenRequesterDoesNotExist() {
        // Given
        Long requesterId = 1L;
        Long requesteeId = 2L;
        ChatRequestDto.Request request = chatRequest(requesteeId, 10L);
        when(memberRepository.findById(requesterId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatRequestService.saveChatRequest(requesterId, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        verify(memberRepository, never()).findById(requesteeId);
        verifyNoInteractions(chattingRequiredRepository, roommateBoardRepository, chattingRoomRepository, chatRoomMemberRepository,
                basicInformationRepository, chattingRequiredAlarmRepository, alarmService);
    }

    @Test
    @DisplayName("피요청자가 없으면 회원 없음 예외를 던지고 채팅 요청 중복 여부를 조회하지 않는다")
    void saveChatRequestThrowsWhenRequesteeDoesNotExist() {
        // Given
        Long requesterId = 1L;
        Long requesteeId = 2L;
        Member requester = Member.builder().id(requesterId).isDelete(false).build();
        ChatRequestDto.Request request = chatRequest(requesteeId, 10L);
        when(memberRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(memberRepository.findById(requesteeId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatRequestService.saveChatRequest(requesterId, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        verifyNoInteractions(chattingRequiredRepository, roommateBoardRepository, chattingRoomRepository, chatRoomMemberRepository,
                basicInformationRepository, chattingRequiredAlarmRepository, alarmService);
    }

    @Test
    @DisplayName("두 회원 사이에 대기 중인 채팅 요청이 이미 있으면 중복 예외를 던지고 게시글과 채팅방을 저장하지 않는다")
    void saveChatRequestThrowsWhenPendingRequestAlreadyExists() {
        // Given
        Long requesterId = 1L;
        Long requesteeId = 2L;
        Member requester = Member.builder().id(requesterId).isDelete(false).build();
        Member requestee = Member.builder().id(requesteeId).isDelete(false).build();
        ChatRequestDto.Request request = chatRequest(requesteeId, 10L);
        ChattingRequired pendingRequest = ChattingRequired.builder()
                .requesterId(requestee.getId())
                .requesteeId(requester.getId())
                .status(ChattingRequiredStatus.PENDING)
                .build();
        when(memberRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(memberRepository.findById(requesteeId)).thenReturn(Optional.of(requestee));
        when(chattingRequiredRepository.findLatest(requester.getId(), requestee.getId())).thenReturn(Optional.of(pendingRequest));

        // When & Then
        assertThatThrownBy(() -> chatRequestService.saveChatRequest(requesterId, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(RequiredErrorCode.CHATTING_DUPLICATE));
        verify(chattingRequiredRepository, never()).save(any(ChattingRequired.class));
        verify(chattingRoomRepository, never()).save(any(ChattingRoom.class));
        verifyNoInteractions(roommateBoardRepository, chatRoomMemberRepository, basicInformationRepository,
                chattingRequiredAlarmRepository, alarmService);
    }

    @Test
    @DisplayName("요청한 게시글이 없으면 게시글 없음 예외를 던지고 채팅 요청과 채팅방을 저장하지 않는다")
    void saveChatRequestThrowsWhenRoommateBoardDoesNotExist() {
        // Given
        Long requesterId = 1L;
        Long requesteeId = 2L;
        Long boardId = 10L;
        Member requester = Member.builder().id(requesterId).isDelete(false).build();
        Member requestee = Member.builder().id(requesteeId).isDelete(false).build();
        ChatRequestDto.Request request = chatRequest(requesteeId, boardId);
        when(memberRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(memberRepository.findById(requesteeId)).thenReturn(Optional.of(requestee));
        when(chattingRequiredRepository.findLatest(requester.getId(), requestee.getId())).thenReturn(Optional.empty());
        when(roommateBoardRepository.findById(boardId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatRequestService.saveChatRequest(requesterId, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(RoommateBoardErrorCode.ROOMMATE_BOARD_NOT_FOUND));
        verify(chattingRequiredRepository, never()).save(any(ChattingRequired.class));
        verify(chattingRoomRepository, never()).save(any(ChattingRoom.class));
        verifyNoInteractions(chatRoomMemberRepository, basicInformationRepository, chattingRequiredAlarmRepository, alarmService);
    }

    @Test
    @DisplayName("피요청자가 채팅 요청을 수락하면 상태를 수락으로 변경하고 요청자에게 알림을 전송한다")
    void acceptRequiredChangesStatusAndPublishesAlarm() {
        // Given
        Long requestId = 1000L;
        Long requesterId = 1L;
        Long requesteeId = 2L;
        Member requester = Member.builder().id(requesterId).isDelete(false).build();
        Member requestee = Member.builder().id(requesteeId).isDelete(false).build();
        ChattingRequired chattingRequired = chatRequest(requester, requestee, ChattingRequiredStatus.PENDING);

        when(chattingRequiredRepository.findById(requestId)).thenReturn(Optional.of(chattingRequired));
        when(basicInformationRepository.findLatestBasicInformation(requestee))
                .thenReturn(Optional.of(basicInformation(requestee, "이수현")));

        when(memberRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(memberRepository.findById(requesteeId)).thenReturn(Optional.of(requestee));

        // When
        ChatRequestDto.Response response = chatRequestService.acceptRequired(requesteeId, requestId);

        // Then
        assertThat(chattingRequired.getStatus()).isEqualTo(ChattingRequiredStatus.ACCEPTED);
        assertThat(response.getUpdatedAt()).isNotNull();
        assertChattingRequiredAlarm(requester, "이수현님이 매칭 요청을 수락했어요", chattingRequired);
        verify(alarmService).sendToClient(eq(requesterId), eq(AlarmType.CHATTING_REQUIRED.name()), any(ChattingRequiredAlarm.class));
    }

    @Test
    @DisplayName("피요청자가 채팅 요청을 거절하면 상태를 거절로 변경하고 요청자에게 알림을 전송한다")
    void rejectRequiredChangesStatusAndPublishesAlarm() {
        // Given
        Long requestId = 1000L;
        Long requesterId = 1L;
        Long requesteeId = 2L;
        Member requester = Member.builder().id(requesterId).isDelete(false).build();
        Member requestee = Member.builder().id(requesteeId).isDelete(false).build();
        ChattingRequired chattingRequired = chatRequest(requester, requestee, ChattingRequiredStatus.PENDING);

        when(chattingRequiredRepository.findById(requestId)).thenReturn(Optional.of(chattingRequired));
        when(basicInformationRepository.findLatestBasicInformation(requestee))
                .thenReturn(Optional.of(basicInformation(requestee, "이수현")));

        when(memberRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(memberRepository.findById(requesteeId)).thenReturn(Optional.of(requestee));

        // When
        ChatRequestDto.Response response = chatRequestService.rejectRequired(requesteeId, requestId);

        // Then
        assertThat(chattingRequired.getStatus()).isEqualTo(ChattingRequiredStatus.REJECTED);
        assertThat(response.getUpdatedAt()).isNotNull();
        assertChattingRequiredAlarm(requester, "이수현님이 매칭 요청을 거절했어요", chattingRequired);
        verify(alarmService).sendToClient(eq(requesterId), eq(AlarmType.CHATTING_REQUIRED.name()), any(ChattingRequiredAlarm.class));
    }

    @Test
    @DisplayName("요청자가 채팅 요청을 취소하면 상태를 취소로 변경하고 피요청자에게 알림을 전송한다")
    void cancelRequiredChangesStatusAndPublishesAlarm() {
        // Given
        Long requestId = 1000L;
        Long requesterId = 1L;
        Long requesteeId = 2L;
        Member requester = Member.builder().id(requesterId).isDelete(false).build();
        Member requestee = Member.builder().id(requesteeId).isDelete(false).build();
        ChattingRequired chattingRequired = chatRequest(requester, requestee, ChattingRequiredStatus.PENDING);

        when(chattingRequiredRepository.findById(requestId)).thenReturn(Optional.of(chattingRequired));
        when(basicInformationRepository.findLatestBasicInformation(requester))
                .thenReturn(Optional.of(basicInformation(requester, "김중민")));

        when(memberRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(memberRepository.findById(requesteeId)).thenReturn(Optional.of(requestee));

        // When
        ChatRequestDto.Response response = chatRequestService.cancelRequired(requesterId, requestId);

        // Then
        assertThat(chattingRequired.getStatus()).isEqualTo(ChattingRequiredStatus.CANCELED);
        assertThat(response.getUpdatedAt()).isNotNull();
        assertChattingRequiredAlarm(requestee, "김중민님이 매칭 요청을 취소했어요", chattingRequired);
        verify(alarmService).sendToClient(eq(requesteeId), eq(AlarmType.CHATTING_REQUIRED.name()), any(ChattingRequiredAlarm.class));
    }

    @Test
    @DisplayName("요청자가 채팅 요청 수락을 시도하면 권한 없음 예외를 던지고 알림을 전송하지 않는다")
    void acceptRequiredRejectsRequesterAccess() {
        // Given
        Long requestId = 1000L;
        Long requesterId = 1L;
        ChattingRequired chattingRequired = chatRequest(
                Member.builder().id(requesterId).isDelete(false).build(),
                Member.builder().id(2L).isDelete(false).build(),
                ChattingRequiredStatus.PENDING
        );

        when(chattingRequiredRepository.findById(requestId)).thenReturn(Optional.of(chattingRequired));

        // When & Then
        assertThatThrownBy(() -> chatRequestService.acceptRequired(requesterId, requestId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(RequiredErrorCode.CHATTING_ACCESS_DENIED));
        assertThat(chattingRequired.getStatus()).isEqualTo(ChattingRequiredStatus.PENDING);
        verifyNoInteractions(basicInformationRepository, chattingRequiredAlarmRepository, alarmService);
    }

    @Test
    @DisplayName("존재하지 않는 채팅 요청을 거절하려 하면 조회 실패 예외를 던지고 알림을 전송하지 않는다")
    void rejectRequiredRejectsMissingRequest() {
        // Given
        Long requestId = 1000L;
        when(chattingRequiredRepository.findById(requestId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatRequestService.rejectRequired(2L, requestId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(RequiredErrorCode.CHATTING_NOT_FOUND));
        verifyNoInteractions(basicInformationRepository, chattingRequiredAlarmRepository, alarmService);
    }

    @Test
    @DisplayName("이미 처리된 채팅 요청을 취소하려 하면 상태 오류 예외를 던지고 알림을 전송하지 않는다")
    void cancelRequiredRejectsInvalidStatus() {
        // Given
        Long requestId = 1000L;
        Long requesterId = 1L;
        ChattingRequired chattingRequired = chatRequest(
                Member.builder().id(requesterId).isDelete(false).build(),
                Member.builder().id(2L).isDelete(false).build(),
                ChattingRequiredStatus.ACCEPTED
        );

        when(chattingRequiredRepository.findById(requestId)).thenReturn(Optional.of(chattingRequired));

        // When & Then
        assertThatThrownBy(() -> chatRequestService.cancelRequired(requesterId, requestId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(RequiredErrorCode.CHATTING_INVALID_STATUS));
        assertThat(chattingRequired.getStatus()).isEqualTo(ChattingRequiredStatus.ACCEPTED);
        verifyNoInteractions(basicInformationRepository, chattingRequiredAlarmRepository, alarmService);
    }

    private ChatRequestDto.Request chatRequest(Long requesteeId, Long boardId) {
        ChatRequestDto.Request request = new ChatRequestDto.Request();
        request.setRequesteeId(requesteeId);
        request.setBoardId(boardId);
        return request;
    }

    private ChattingRequired chatRequest(Member requester, Member requestee, ChattingRequiredStatus status) {
        return ChattingRequired.builder()
                .requesterId(requester.getId())
                .requesteeId(requestee.getId())
                .status(status)
                .build();
    }

    private ChattingRequired chatRequest(
            Long id,
            Member requester,
            Member requestee,
            ChattingRequiredStatus status,
            LocalDateTime createdAt
    ) {
        ChattingRequired chattingRequired = chatRequest(requester, requestee, status);
        ReflectionTestUtils.setField(chattingRequired, "id", id);
        ReflectionTestUtils.setField(chattingRequired, "createdAt", createdAt);
        return chattingRequired;
    }

    private ChattingRoomBasicInfoRow basicInfoRow(
            Long memberId,
            String name,
            LocalDate birth,
            Gender gender,
            String profileImageUrl
    ) {
        return new ChattingRoomBasicInfoRow(memberId, name, birth, gender, profileImageUrl);
    }

    private MatchingLifestyleRow lifestyleRow(
            Long memberId,
            Long lifestyleId,
            String name,
            String value,
            String description,
            LifePatternType type
    ) {
        return new MatchingLifestyleRow(
                memberId, lifestyleId, lifestyleId, lifestyleId, name, value, description, type, null);
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

    private void assertChattingRequiredAlarm(Member receiver, String message, ChattingRequired chattingRequired) {
        ArgumentCaptor<ChattingRequiredAlarm> alarmCaptor = ArgumentCaptor.forClass(ChattingRequiredAlarm.class);
        verify(alarmService).sendToClient(eq(receiver.getId()), eq(AlarmType.CHATTING_REQUIRED.name()), alarmCaptor.capture());
        assertThat(alarmCaptor.getValue().getMemberId()).isEqualTo(receiver.getId());
        assertThat(alarmCaptor.getValue().getTitle()).isEqualTo(message);
        assertThat(alarmCaptor.getValue().getContents()).isEqualTo(message);
        assertThat(alarmCaptor.getValue().getType()).isEqualTo(AlarmType.CHATTING_REQUIRED);
        assertThat(alarmCaptor.getValue().getChattingRequiredId()).isEqualTo(chattingRequired.getId());
    }
}
