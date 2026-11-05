package org.example.knockin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.example.knockin.alarm.service.impl.PushNotificationServiceImpl;
import org.example.knockin.verification.service.impl.AuthenticationServiceImpl;
import org.example.knockin.board.service.impl.RoommateBoardServiceImpl;
import org.example.knockin.chat.dto.ChatMessageDto;
import org.example.knockin.chat.dto.ChatRoomCreateDto;
import org.example.knockin.chat.dto.ChatRoomDetailDto;
import org.example.knockin.chat.dto.ChatRoomDto;
import org.example.knockin.chat.dto.ChatRoomImageDto;
import org.example.knockin.chat.dto.ChatRoomLeftEvent;
import org.example.knockin.chat.dto.ChatRoomListDto;
import org.example.knockin.chat.dto.ChatRoomMessageEvent;
import org.example.knockin.chat.dto.ChatSocketResponse;
import org.example.knockin.chat.service.impl.ChatRoomFileServiceImpl;
import org.example.knockin.chat.service.impl.ChatRoomMemberServiceImpl;
import org.example.knockin.chat.service.impl.ChatRoomMessageServiceImpl;
import org.example.knockin.chat.service.impl.ChatServiceImpl;
import org.example.knockin.chat.service.impl.ChattingRequiredServiceImpl;
import org.example.knockin.chat.service.impl.ChattingRoomServiceImpl;
import org.example.knockin.chat.service.impl.ChattingScoreServiceImpl;
import org.example.knockin.chat.dto.EventType;
import org.example.knockin.chat.dto.MessageType;
import org.example.knockin.mate.dto.RoommateRequestDto.RoommateMatchingRequiredInfo;
import org.example.knockin.alarm.entity.AlarmSettingType;
import org.example.knockin.verification.entity.AuthenticationType;
import org.example.knockin.chat.entity.ChatRoomFile;
import org.example.knockin.chat.entity.ChatRoomMember;
import org.example.knockin.chat.entity.ChatRoomMessage;
import org.example.knockin.chat.entity.ChattingRequired;
import org.example.knockin.chat.entity.ChattingRequiredStatus;
import org.example.knockin.chat.entity.ChattingRoom;
import org.example.knockin.chat.entity.ChattingScore;
import org.example.knockin.member.repository.BasicInformationFileRepository;
import org.example.knockin.member.service.impl.BasicInformationServiceImpl;
import org.example.knockin.member.service.impl.BlockServiceImpl;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.example.knockin.meta.entity.File;
import org.example.knockin.meta.entity.FileType;
import org.example.knockin.member.entity.BasicInformation;
import org.example.knockin.member.entity.Gender;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberState;
import org.example.knockin.member.entity.State;
import org.example.knockin.mate.entity.RoommateMatchingRequired;
import org.example.knockin.mate.entity.RoommateRequiredStatus;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.ChattingErrorCode;
import org.example.knockin.global.exception.FileErrorCode;
import org.example.knockin.global.exception.MemberErrorCode;
import org.example.knockin.global.util.DateUtils;
import org.example.knockin.board.repository.RoommateBoardRepository;
import org.example.knockin.chat.repository.ChatRoomFileRepository;
import org.example.knockin.chat.repository.ChatRoomMemberRepository;
import org.example.knockin.chat.repository.ChatRoomMessageRepository;
import org.example.knockin.chat.repository.ChattingRequiredRepository;
import org.example.knockin.chat.repository.ChattingRoomRepository;
import org.example.knockin.chat.repository.ChattingScoreRepository;
import org.example.knockin.mate.service.impl.RoommateMatchingRequiredServiceImpl;
import org.example.knockin.meta.repository.FileRepository;
import org.example.knockin.member.repository.BasicInformationRepository;
import org.example.knockin.member.repository.MemberRepository;
import org.example.knockin.member.repository.StateRepository;
import org.example.knockin.member.repository.row.ChattingRoomBasicInfoRow;
import org.example.knockin.verification.repository.row.MemberAuthenticationRow;
import org.example.knockin.chat.repository.row.ChatRoomListRow;
import org.example.knockin.chat.repository.row.ChatRoomUnreadCountRow;
import org.example.knockin.mate.repository.RoommateMatchingRequiredRepository;
import org.example.knockin.mate.service.impl.MyRoomMateServiceImpl;
import org.example.knockin.meta.service.FileService;
import org.example.knockin.util.service.RoommateScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@DisplayName("채팅 서비스")
class ChatServiceImplTest {

    @Mock
    private ChattingRoomRepository chattingRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private ChatRoomMessageRepository chatRoomMessageRepository;

    @Mock
    private FileService fileService;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private ChatRoomFileRepository chatRoomFileRepository;

    @Mock
    private BasicInformationRepository basicInformationRepository;

    @Mock
    private RoommateMatchingRequiredRepository roommateMatchingRequiredRepository;

    @Mock
    private RoommateBoardRepository roommateBoardRepository;

    @Mock
    private ChattingRequiredRepository chattingRequiredRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private StateRepository stateRepository;

    @Mock
    private RoommateScoreService roommateScoreService;

    @Mock
    private ChattingScoreRepository chattingScoreRepository;

    @Mock
    private BlockServiceImpl blockService;

    @Mock
    private PushNotificationServiceImpl pushNotificationService;

    @Mock
    private AuthenticationServiceImpl authenticationService;

    @Mock
    private MyRoomMateServiceImpl myRoomMateService;

    @InjectMocks
    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        MemberServiceImpl memberService = new MemberServiceImpl(
                memberRepository,
                null,
                null,
                null,
                null,
                stateRepository,
                null,
                null,
                null
        );
        org.mockito.Mockito.lenient()
                .when(stateRepository.findByMemberId(any(Long.class)))
                .thenReturn(List.of(State.builder().states(MemberState.ACTIVE).build()));
        BasicInformationServiceImpl basicInformationService = new BasicInformationServiceImpl(basicInformationRepository, org.mockito.Mockito.mock(
                BasicInformationFileRepository.class));
        RoommateBoardServiceImpl roommateBoardService = new RoommateBoardServiceImpl(
                roommateBoardRepository,
                memberService,
                null,
                roommateScoreService,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        chatService = new ChatServiceImpl(
                new ChattingRoomServiceImpl(chattingRoomRepository),
                new ChatRoomMemberServiceImpl(chatRoomMemberRepository),
                messagingTemplate,
                publisher,
                new ChatRoomMessageServiceImpl(chatRoomMessageRepository),
                fileService,
                new ChatRoomFileServiceImpl(chatRoomFileRepository),
                basicInformationService,
                new RoommateMatchingRequiredServiceImpl(roommateMatchingRequiredRepository),
                roommateBoardService,
                new ChattingRequiredServiceImpl(chattingRequiredRepository),
                memberService,
                roommateScoreService,
                new ChattingScoreServiceImpl(chattingScoreRepository),
                blockService,
                pushNotificationService,
                authenticationService,
                myRoomMateService
        );
        ReflectionTestUtils.setField(chatService, "chatRoomLimitPerMember", 15L);
    }

    @Test
    @DisplayName("회원 식별자로 조회한 채팅방 Row에 안 읽은 수와 인증 타입을 조립한다")
    void getChatRoomListBuildsResponsesWithoutPerRoomQueries() {
        // Given
        Long memberId = 1L;
        List<ChatRoomListRow> rows = List.of(
                chatRoomRow(10L, 2L, "상대방A", "profile-a.jpg", LocalDateTime.of(2026, 6, 18, 10, 0)),
                chatRoomRow(20L, 3L, "상대방B", "profile-b.jpg", LocalDateTime.of(2026, 6, 18, 11, 0))
        );
        when(chattingRoomRepository.findListRowsByMemberId(memberId)).thenReturn(rows);
        when(chatRoomMessageRepository.findUnreadMessageCounts(memberId, List.of(10L, 20L)))
                .thenReturn(List.of(new ChatRoomUnreadCountRow(10L, 3L)));
        when(authenticationService.findAcceptedByMemberIds(List.of(2L, 3L)))
                .thenReturn(List.of(
                        new MemberAuthenticationRow(2L, AuthenticationType.STUDENT),
                        new MemberAuthenticationRow(2L, AuthenticationType.COMPANY)
                ));

        // When
        List<ChatRoomListDto.Response> responses = chatService.getChatRoomList(memberId);

        // Then
        assertThat(responses).extracting(ChatRoomListDto.Response::getChatRoomId)
                .containsExactly(10L, 20L);
        assertThat(responses).extracting(ChatRoomListDto.Response::getMemberName)
                .containsExactly("상대방A", "상대방B");
        assertThat(responses).extracting(ChatRoomListDto.Response::getMemberProfileImageUrl)
                .containsExactly("profile-a.jpg", "profile-b.jpg");
        assertThat(responses).extracting(ChatRoomListDto.Response::getMessageCount)
                .containsExactly(3, 0);
        assertThat(responses.getFirst().getAuthenticationTypes())
                .containsExactly(AuthenticationType.STUDENT, AuthenticationType.COMPANY);
        assertThat(responses.get(1).getAuthenticationTypes()).isEmpty();
        verify(chattingRoomRepository).findListRowsByMemberId(memberId);
        verify(chatRoomMessageRepository).findUnreadMessageCounts(memberId, List.of(10L, 20L));
        verify(authenticationService).findAcceptedByMemberIds(List.of(2L, 3L));
    }

    @Test
    @DisplayName("참여한 채팅방이 없으면 빈 목록을 반환한다")
    void getChatRoomListReturnsEmptyListWhenMemberHasNoRooms() {
        // Given
        Long memberId = 1L;
        when(chattingRoomRepository.findListRowsByMemberId(memberId)).thenReturn(List.of());

        // When
        List<ChatRoomListDto.Response> responses = chatService.getChatRoomList(memberId);

        // Then
        assertThat(responses).isEmpty();
        verify(chattingRoomRepository).findListRowsByMemberId(memberId);
        verifyNoInteractions(authenticationService);
    }

    @Test
    @DisplayName("차단 관계여도 채팅방 상세와 기존 내역을 유지하고 차단 여부를 반환한다")
    void getChatRoomDetailReturnsProfileMessagesAndRoommateRequests() {
        // Given
        Long chatRoomId = 10L;
        Long memberId = 1L;
        LocalDate opponentBirth = LocalDate.now().minusYears(25);
        ChattingRoom chattingRoom = chattingRoom();
        Member me = member(memberId);
        Member opponent = member(2L);
        ChatRoomMember roomMember = activeRoomMember(me, chattingRoom);
        List<ChatRoomDetailDto.ChatMessage> messages = List.of(
                new ChatRoomDetailDto.ChatMessage(
                        100L,
                        memberId,
                        "안녕하세요",
                        LocalDateTime.of(2026, 6, 23, 10, 0),
                        MessageType.TEXT,
                        null
                )
        );
        List<RoommateMatchingRequiredInfo> matchingRequiredList = List.of(
                RoommateMatchingRequiredInfo.builder()
                        .requiredId(200L)
                        .requesterMemberId(memberId)
                        .requesteeMemberId(opponent.getId())
                        .status(RoommateRequiredStatus.PENDING)
                        .createdAt(LocalDateTime.of(2026, 6, 23, 10, 30))
                        .updatedAt(LocalDateTime.of(2026, 6, 23, 10, 30))
                        .build()
        );
        ChattingScore chattingScore = ChattingScore.builder().score(87).build();
        when(chattingRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chattingRoom));
        when(chatRoomMemberRepository.findActiveMemberByRoomIdAndMemberId(chatRoomId, memberId))
                .thenReturn(Optional.of(roomMember));
        when(chatRoomMemberRepository.findPartnerMember(roomMember, chatRoomId)).thenReturn(opponent);
        when(basicInformationRepository.findChattingRoomBasicInfoRow(opponent.getId()))
                .thenReturn(Optional.of(new ChattingRoomBasicInfoRow(
                        opponent.getId(),
                        "상대방",
                        opponentBirth,
                        Gender.FEMALE,
                        "opponent-profile.jpg"
                )));
        when(chatRoomMessageRepository.markUnreadMessagesAsRead(chatRoomId, memberId)).thenReturn(2L);
        when(chatRoomMessageRepository.findChatMessageDto(chatRoomId)).thenReturn(messages);
        when(roommateMatchingRequiredRepository.findRequiredDto(chattingRoom)).thenReturn(matchingRequiredList);
        when(chattingScoreRepository.findOneByChattingRequiredIdAndMemberId(100L, memberId))
                .thenReturn(Optional.of(chattingScore));
        when(blockService.isBlockedBetween(memberId, opponent.getId())).thenReturn(true);
        when(myRoomMateService.isExistRoomMate(opponent)).thenReturn(true);

        // When
        ChatRoomDetailDto.Response response = chatService.getChatRoomDetail(chatRoomId, memberId);

        // Then
        assertThat(response.getOpponentProfile().getId()).isEqualTo(opponent.getId());
        assertThat(response.getOpponentProfile().getName()).isEqualTo("상대방");
        assertThat(response.getOpponentProfile().getAge()).isEqualTo(DateUtils.calculateAge(opponentBirth));
        assertThat(response.getOpponentProfile().getGender()).isEqualTo(Gender.FEMALE);
        assertThat(response.getOpponentProfile().getMemberProfileImageUrl()).isEqualTo("opponent-profile.jpg");
        assertThat(response.getOpponentProfile().getScore()).isEqualTo(87);
        assertThat(response.getMessages()).isSameAs(messages);
        assertThat(response.getMatchingRequiredList()).isSameAs(matchingRequiredList);
        assertThat(response.isBlocked()).isTrue();
        assertThat(response.isOpponentHasRoommate()).isTrue();
        verify(myRoomMateService).isExistRoomMate(opponent);
        verify(roommateScoreService, never()).calculateSimpleScore(memberId, opponent.getId());

        InOrder detailOrder = inOrder(
                chattingRoomRepository,
                chatRoomMemberRepository,
                chatRoomMessageRepository
        );
        detailOrder.verify(chattingRoomRepository).findById(chatRoomId);
        detailOrder.verify(chatRoomMemberRepository).findActiveMemberByRoomIdAndMemberId(chatRoomId, memberId);
        detailOrder.verify(chatRoomMessageRepository).markUnreadMessagesAsRead(chatRoomId, memberId);
        detailOrder.verify(chatRoomMemberRepository).findPartnerMember(roomMember, chatRoomId);
        detailOrder.verify(chatRoomMessageRepository).findChatMessageDto(chatRoomId);
    }

    @Test
    @DisplayName("채팅방 상세 조회 시 활성 채팅방 멤버가 아니면 실패한다")
    void getChatRoomDetailRejectsMemberWhoIsNotActiveRoomMember() {
        // Given
        Long chatRoomId = 10L;
        Long memberId = 1L;
        ChattingRoom chattingRoom = chattingRoom();
        when(chattingRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chattingRoom));
        when(chatRoomMemberRepository.findActiveMemberByRoomIdAndMemberId(chatRoomId, memberId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatService.getChatRoomDetail(chatRoomId, memberId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ChattingErrorCode.ROOM_MEMBER_NOT_FOUND));
        verify(chatRoomMemberRepository, never()).findPartnerMember(any(), eq(chatRoomId));
        verifyNoInteractions(
                basicInformationRepository,
                chatRoomMessageRepository,
                roommateMatchingRequiredRepository,
                myRoomMateService
        );
    }

    @Test
    @DisplayName("읽음 처리할 메시지가 없어도 채팅방 상세 조회는 정상 완료한다")
    void getChatRoomDetailSucceedsWhenThereAreNoUnreadMessages() {
        // Given
        Long chatRoomId = 10L;
        Long memberId = 1L;
        LocalDate opponentBirth = LocalDate.now().minusYears(25);
        ChattingRoom chattingRoom = chattingRoom();
        Member me = member(memberId);
        Member opponent = member(2L);
        ChatRoomMember roomMember = activeRoomMember(me, chattingRoom);

        when(chattingRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chattingRoom));
        when(chatRoomMemberRepository.findActiveMemberByRoomIdAndMemberId(chatRoomId, memberId))
                .thenReturn(Optional.of(roomMember));
        when(chatRoomMemberRepository.findPartnerMember(roomMember, chatRoomId)).thenReturn(opponent);
        when(basicInformationRepository.findChattingRoomBasicInfoRow(opponent.getId()))
                .thenReturn(Optional.of(new ChattingRoomBasicInfoRow(
                        opponent.getId(),
                        "상대방",
                        opponentBirth,
                        Gender.FEMALE,
                        "opponent-profile.jpg"
                )));
        when(chatRoomMessageRepository.markUnreadMessagesAsRead(chatRoomId, memberId)).thenReturn(0L);
        when(chatRoomMessageRepository.findChatMessageDto(chatRoomId)).thenReturn(List.of());
        when(roommateMatchingRequiredRepository.findRequiredDto(chattingRoom)).thenReturn(List.of());
        when(chattingScoreRepository.findOneByChattingRequiredIdAndMemberId(100L, memberId))
                .thenReturn(Optional.empty());
        when(roommateScoreService.calculateSimpleScore(memberId, opponent.getId())).thenReturn(100);
        when(blockService.isBlockedBetween(memberId, opponent.getId())).thenReturn(false);

        // When
        ChatRoomDetailDto.Response response = chatService.getChatRoomDetail(chatRoomId, memberId);

        // Then
        assertThat(response.getMessages()).isEmpty();
        assertThat(response.getMatchingRequiredList()).isEmpty();
        assertThat(response.getOpponentProfile().getId()).isEqualTo(opponent.getId());
        assertThat(response.isBlocked()).isFalse();
        assertThat(response.isOpponentHasRoommate()).isFalse();
        verify(chatRoomMessageRepository).markUnreadMessagesAsRead(chatRoomId, memberId);
        verify(myRoomMateService).isExistRoomMate(opponent);
    }

    @Test
    @DisplayName("채팅방 상세 조회 시 상대방 기본 정보가 없으면 실패한다")
    void getChatRoomDetailRejectsWhenOpponentBasicInformationMissing() {
        // Given
        Long chatRoomId = 10L;
        Long memberId = 1L;
        ChattingRoom chattingRoom = chattingRoom();
        Member me = member(memberId);
        Member opponent = member(2L);
        ChatRoomMember roomMember = activeRoomMember(me, chattingRoom);
        when(chattingRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chattingRoom));
        when(chatRoomMemberRepository.findActiveMemberByRoomIdAndMemberId(chatRoomId, memberId))
                .thenReturn(Optional.of(roomMember));
        when(chatRoomMemberRepository.findPartnerMember(roomMember, chatRoomId)).thenReturn(opponent);
        when(basicInformationRepository.findChattingRoomBasicInfoRow(opponent.getId())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatService.getChatRoomDetail(chatRoomId, memberId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.BASIC_INFO_NOT_FOUND));
        verify(chatRoomMessageRepository).markUnreadMessagesAsRead(chatRoomId, memberId);
        verify(chatRoomMessageRepository, never()).findChatMessageDto(chatRoomId);
        verifyNoInteractions(roommateMatchingRequiredRepository);
        verifyNoInteractions(myRoomMateService);
    }

    @Test
    @DisplayName("채팅방 생성 요청이 유효하면 승인된 요청과 채팅방과 첫 메시지를 저장한다")
    void createChattingRoomCreatesAcceptedRequestRoomMembersAndFirstMessage() {
        // Given
        Long requesterId = 1L;
        Long requesteeId = 2L;
        Long boardId = 10L;
        Member requester = member(requesterId);
        Member requestee = member(requesteeId);
        ChatRoomCreateDto.Request request = chatRoomCreateRequest(requesteeId, boardId, "안녕하세요");
        LocalDateTime messageCreatedAt = LocalDateTime.of(2026, 6, 24, 10, 0);

        when(memberRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(memberRepository.findById(requesteeId)).thenReturn(Optional.of(requestee));
        when(chattingRoomRepository.existsActiveRoomBetweenMembers(requesterId, requesteeId)).thenReturn(false);
        when(chattingRoomRepository.countActiveRoomsByMemberId(requesterId)).thenReturn(14L);
        when(chattingRoomRepository.countActiveRoomsByMemberId(requesteeId)).thenReturn(0L);
        when(chattingRequiredRepository.save(any(ChattingRequired.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(chattingRoomRepository.save(any(ChattingRoom.class)))
                .thenAnswer(invocation -> persistedChattingRoom(invocation.getArgument(0), 100L));
        when(chatRoomMemberRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(chatRoomMessageRepository.save(any(ChatRoomMessage.class)))
                .thenAnswer(invocation -> persistedMessage(invocation.getArgument(0), messageCreatedAt));
        List<ChattingScore> chattingScores = List.of(ChattingScore.builder().score(80).build());
        when(roommateScoreService.createChattingScores(any(ChattingRequired.class))).thenReturn(chattingScores);

        // When
        ChatRoomCreateDto.Response response = chatService.createChattingRoom(requesterId, request);

        // Then
        assertThat(response.getChatRoomId()).isEqualTo(100L);
        assertThat(response.getUpdatedAt()).isEqualTo(messageCreatedAt);

        ArgumentCaptor<ChattingRequired> requiredCaptor = ArgumentCaptor.forClass(ChattingRequired.class);
        verify(chattingRequiredRepository).save(requiredCaptor.capture());
        assertThat(requiredCaptor.getValue().getRequester()).isSameAs(requester);
        assertThat(requiredCaptor.getValue().getRequestee()).isSameAs(requestee);
        assertThat(requiredCaptor.getValue().getRoommateBoard()).isNull();
        assertThat(requiredCaptor.getValue().getStatus()).isEqualTo(ChattingRequiredStatus.ACCEPTED);

        ArgumentCaptor<ChattingRoom> roomCaptor = ArgumentCaptor.forClass(ChattingRoom.class);
        verify(chattingRoomRepository).save(roomCaptor.capture());
        assertThat(roomCaptor.getValue().getChattingRequired()).isSameAs(requiredCaptor.getValue());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<ChatRoomMember>> membersCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(chatRoomMemberRepository).saveAll(membersCaptor.capture());
        List<ChatRoomMember> members = ((List<ChatRoomMember>) membersCaptor.getValue());
        assertThat(members).hasSize(2);
        assertThat(members).extracting(ChatRoomMember::getMember).containsExactly(requester, requestee);
        assertThat(members).extracting(ChatRoomMember::getIsLeft).containsExactly(false, false);

        ArgumentCaptor<ChatRoomMessage> messageCaptor = ArgumentCaptor.forClass(ChatRoomMessage.class);
        verify(chatRoomMessageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getContents()).isEqualTo("안녕하세요");
        assertThat(messageCaptor.getValue().getMember()).isSameAs(requester);
        assertThat(messageCaptor.getValue().getChattingRoom().getId()).isEqualTo(100L);
        assertThat(messageCaptor.getValue().getType()).isEqualTo(MessageType.TEXT);
        verify(roommateScoreService).createChattingScores(requiredCaptor.getValue());
        verify(chattingScoreRepository).saveAll(chattingScores);
        verifyNoInteractions(roommateBoardRepository);
    }

    @Test
    @DisplayName("두 회원 사이에 활성 채팅방이 이미 있으면 채팅방을 생성하지 않는다")
    void createChattingRoomRejectsDuplicateActiveRoom() {
        // Given
        Long requesterId = 1L;
        Long requesteeId = 2L;
        Member requester = member(requesterId);
        Member requestee = member(requesteeId);
        ChatRoomCreateDto.Request request = chatRoomCreateRequest(requesteeId, null, "안녕하세요");

        when(memberRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(memberRepository.findById(requesteeId)).thenReturn(Optional.of(requestee));
        when(chattingRoomRepository.existsActiveRoomBetweenMembers(requesterId, requesteeId)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> chatService.createChattingRoom(requesterId, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ChattingErrorCode.ROOM_DUPLICATE));
        verify(chattingRoomRepository, never()).countActiveRoomsByMemberId(any());
        verifyNoInteractions(roommateBoardRepository, chattingRequiredRepository, chatRoomMemberRepository, chatRoomMessageRepository);
    }

    @Test
    @DisplayName("차단 관계인 두 회원은 새 채팅방을 생성할 수 없다")
    void createChattingRoomRejectsBlockedMembers() {
        // Given
        Long requesterId = 1L;
        Long requesteeId = 2L;
        Member requester = member(requesterId);
        Member requestee = member(requesteeId);
        ChatRoomCreateDto.Request request = chatRoomCreateRequest(requesteeId, null, "안녕하세요");
        when(memberRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(memberRepository.findById(requesteeId)).thenReturn(Optional.of(requestee));
        when(blockService.isBlockedBetween(requesterId, requesteeId)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> chatService.createChattingRoom(requesterId, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ChattingErrorCode.MESSAGE_BLOCKED));
        verifyNoInteractions(
                roommateBoardRepository,
                chattingRequiredRepository,
                chatRoomMemberRepository,
                chatRoomMessageRepository
        );
        verify(chattingRoomRepository, never()).existsActiveRoomBetweenMembers(any(), any());
    }

    @Test
    @DisplayName("요청자나 피요청자의 활성 채팅방이 15개 이상이면 채팅방을 생성하지 않는다")
    void createChattingRoomRejectsMemberOverRoomLimit() {
        // Given
        Long requesterId = 1L;
        Long requesteeId = 2L;
        Member requester = member(requesterId);
        Member requestee = member(requesteeId);
        ChatRoomCreateDto.Request request = chatRoomCreateRequest(requesteeId, null, "안녕하세요");

        when(memberRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(memberRepository.findById(requesteeId)).thenReturn(Optional.of(requestee));
        when(chattingRoomRepository.existsActiveRoomBetweenMembers(requesterId, requesteeId)).thenReturn(false);
        when(chattingRoomRepository.countActiveRoomsByMemberId(requesterId)).thenReturn(15L);
        when(chattingRoomRepository.countActiveRoomsByMemberId(requesteeId)).thenReturn(0L);

        // When & Then
        assertThatThrownBy(() -> chatService.createChattingRoom(requesterId, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ChattingErrorCode.ROOM_LIMIT_EXCEEDED));
        verifyNoInteractions(roommateBoardRepository, chattingRequiredRepository, chatRoomMemberRepository, chatRoomMessageRepository);
    }

    @Test
    @DisplayName("채팅방 이미지 파일을 업로드하고 URL을 반환한다")
    void uploadImageUploadsFileAndReturnsUrl() throws IOException {
        // Given
        Long chatRoomId = 10L;
        Long memberId = 1L;
        Member member = member(memberId);
        Member opponent = member(2L);
        ChatRoomMember roomMember = activeRoomMember(member, chattingRoom());
        MultipartFile multipartFile = multipartFile(false);
        File file = chatImage("chat-image.jpg");
        when(chatRoomMemberRepository.findActiveMemberByRoomIdAndMemberId(chatRoomId, memberId))
                .thenReturn(Optional.of(roomMember));
        when(chatRoomMemberRepository.findPartnerMember(roomMember, chatRoomId)).thenReturn(opponent);
        when(fileService.save(multipartFile, FileType.CHAT_ROOM_IMAGE)).thenReturn(file);

        // When
        ChatRoomImageDto.Response response = chatService.uploadImage(
                chatRoomId,
                memberId,
                multipartFile
        );

        // Then
        assertThat(response.getImageUrl()).isEqualTo("chat-image.jpg");
        InOrder inOrder = inOrder(chatRoomMemberRepository, blockService, fileService);
        inOrder.verify(chatRoomMemberRepository).findActiveMemberByRoomIdAndMemberId(chatRoomId, memberId);
        inOrder.verify(chatRoomMemberRepository).findPartnerMember(roomMember, chatRoomId);
        inOrder.verify(blockService).isBlockedBetween(memberId, opponent.getId());
        inOrder.verify(fileService).save(multipartFile, FileType.CHAT_ROOM_IMAGE);
        verifyNoInteractions(fileRepository);
    }

    @Test
    @DisplayName("차단 관계에서는 채팅방 이미지도 업로드할 수 없다")
    void uploadImageRejectsBlockedMembers() {
        // Given
        Long chatRoomId = 10L;
        Long memberId = 1L;
        Member member = member(memberId);
        Member opponent = member(2L);
        ChatRoomMember roomMember = activeRoomMember(member, chattingRoom());
        MultipartFile multipartFile = multipartFile(false);
        when(chatRoomMemberRepository.findActiveMemberByRoomIdAndMemberId(chatRoomId, memberId))
                .thenReturn(Optional.of(roomMember));
        when(chatRoomMemberRepository.findPartnerMember(roomMember, chatRoomId)).thenReturn(opponent);
        when(blockService.isBlockedBetween(memberId, opponent.getId())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> chatService.uploadImage(chatRoomId, memberId, multipartFile))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ChattingErrorCode.MESSAGE_BLOCKED));
        verifyNoInteractions(fileService, fileRepository);
    }

    @Test
    @DisplayName("채팅방 이미지 업로드 요청에 파일이 없으면 실패한다")
    void uploadImageRejectsNullFile() {
        assertThatThrownBy(() -> chatService.uploadImage(10L, 1L, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FILE_EMPTY));
        verifyNoInteractions(chatRoomMemberRepository, fileService, fileRepository);
    }

    @Test
    @DisplayName("채팅방 이미지 업로드 요청 파일이 비어 있으면 실패한다")
    void uploadImageRejectsEmptyFile() {
        // Given
        MultipartFile emptyFile = multipartFile(true);

        // When & Then
        assertThatThrownBy(() -> chatService.uploadImage(10L, 1L, emptyFile))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FILE_EMPTY));
        verifyNoInteractions(chatRoomMemberRepository, fileService, fileRepository);
    }

    @Test
    @DisplayName("채팅방 이미지 업로드 중 실패하면 업로드 실패 예외를 던진다")
    void uploadImageThrowsWhenFileUploadFails() throws IOException {
        // Given
        Long chatRoomId = 10L;
        Long memberId = 1L;
        Member member = member(memberId);
        Member opponent = member(2L);
        ChatRoomMember roomMember = activeRoomMember(member, chattingRoom());
        MultipartFile multipartFile = multipartFile(false);
        when(chatRoomMemberRepository.findActiveMemberByRoomIdAndMemberId(chatRoomId, memberId))
                .thenReturn(Optional.of(roomMember));
        when(chatRoomMemberRepository.findPartnerMember(roomMember, chatRoomId)).thenReturn(opponent);
        when(fileService.save(multipartFile, FileType.CHAT_ROOM_IMAGE))
                .thenThrow(new IOException("upload failed"));

        // When & Then
        assertThatThrownBy(() -> chatService.uploadImage(chatRoomId, memberId, multipartFile))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FILE_UPLOAD_FAILED));
        verifyNoInteractions(fileRepository);
    }

    @Test
    @DisplayName("텍스트 메시지 전송 시 메시지를 저장하고 커밋 후 발행할 이벤트를 등록한다")
    void sendTextMessageSavesMessageAndPublishesEvent() {
        // Given
        Long chatId = 10L;
        Long senderId = 1L;
        Member member = member(senderId);
        Member opponent = member(2L);
        ChattingRoom chattingRoom = chattingRoom();
        ChatRoomMember roomMember = activeRoomMember(member, chattingRoom);
        ChatMessageDto.Request request = textMessageRequest();
        when(chatRoomMemberRepository.findActiveMemberByRoomIdAndMemberId(chatId, senderId))
                .thenReturn(Optional.of(roomMember));
        when(chatRoomMemberRepository.findPartnerMember(roomMember, chatId)).thenReturn(opponent);
        when(chattingRoomRepository.findById(chatId)).thenReturn(Optional.of(chattingRoom));
        when(chatRoomMessageRepository.save(any(ChatRoomMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(basicInformationRepository.findLatestBasicInformation(member))
                .thenReturn(Optional.of(basicInformation(member, "김중민")));

        // When
        chatService.sendUserMessage(chatId, request, senderId);

        // Then
        ArgumentCaptor<ChatRoomMessage> messageCaptor = ArgumentCaptor.forClass(ChatRoomMessage.class);
        verify(chatRoomMessageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getContents()).isEqualTo("안녕하세요");
        assertThat(messageCaptor.getValue().getMember()).isSameAs(member);
        assertThat(messageCaptor.getValue().getChattingRoom()).isSameAs(chattingRoom);
        assertThat(messageCaptor.getValue().getType()).isEqualTo(MessageType.TEXT);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        ChatRoomMessageEvent event = (ChatRoomMessageEvent) eventCaptor.getValue();
        assertThat(event.chatRoomId()).isEqualTo(chatId);
        assertThat(event.senderId()).isEqualTo(senderId);
        assertThat(event.clientMessageId()).isEqualTo("client-message-id");
        assertThat(event.messageType()).isEqualTo(MessageType.TEXT);
        assertThat(event.message()).isEqualTo("안녕하세요");
        assertThat(event.imageUrl()).isNull();
        verify(pushNotificationService).send(
                opponent,
                AlarmSettingType.NOTIFICATION,
                "김중민",
                "안녕하세요",
                "knockinrn://chat/10"
        );
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("이미지 메시지 전송 시 메시지와 파일 연결을 저장하고 커밋 후 발행할 이벤트를 등록한다")
    void sendImageMessageSavesMessageFileAndPublishesEvent() {
        // Given
        Long chatId = 10L;
        Long senderId = 1L;
        Member member = member(senderId);
        Member opponent = member(2L);
        ChattingRoom chattingRoom = chattingRoom();
        ChatRoomMember roomMember = activeRoomMember(member, chattingRoom);
        ChatMessageDto.Request request = imageMessageRequest("chat-image.jpg");
        File file = chatImage("chat-image.jpg");
        ChatRoomMessage savedMessage = ChatRoomMessage.builder()
                .contents("사진을 보냈습니다.")
                .member(member)
                .chattingRoom(chattingRoom)
                .type(MessageType.IMAGE)
                .build();
        when(chatRoomMemberRepository.findActiveMemberByRoomIdAndMemberId(chatId, senderId))
                .thenReturn(Optional.of(roomMember));
        when(chatRoomMemberRepository.findPartnerMember(roomMember, chatId)).thenReturn(opponent);
        when(chattingRoomRepository.findById(chatId)).thenReturn(Optional.of(chattingRoom));
        when(fileService.findBySavedFileNameAndType("chat-image.jpg", FileType.CHAT_ROOM_IMAGE))
                .thenReturn(file);
        when(chatRoomMessageRepository.save(any(ChatRoomMessage.class))).thenReturn(savedMessage);
        when(chatRoomFileRepository.save(any(ChatRoomFile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(basicInformationRepository.findLatestBasicInformation(member))
                .thenReturn(Optional.of(basicInformation(member, "김중민")));

        // When
        chatService.sendUserMessage(chatId, request, senderId);

        // Then
        ArgumentCaptor<ChatRoomMessage> messageCaptor = ArgumentCaptor.forClass(ChatRoomMessage.class);
        verify(chatRoomMessageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getContents()).isEqualTo("사진을 보냈습니다.");
        assertThat(messageCaptor.getValue().getMember()).isSameAs(member);
        assertThat(messageCaptor.getValue().getChattingRoom()).isSameAs(chattingRoom);
        assertThat(messageCaptor.getValue().getType()).isEqualTo(MessageType.IMAGE);

        ArgumentCaptor<ChatRoomFile> chatRoomFileCaptor = ArgumentCaptor.forClass(ChatRoomFile.class);
        verify(chatRoomFileRepository).save(chatRoomFileCaptor.capture());
        assertThat(chatRoomFileCaptor.getValue().getFile()).isSameAs(file);
        assertThat(chatRoomFileCaptor.getValue().getChatRoomMessage()).isSameAs(savedMessage);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        ChatRoomMessageEvent event = (ChatRoomMessageEvent) eventCaptor.getValue();
        assertThat(event.messageType()).isEqualTo(MessageType.IMAGE);
        assertThat(event.imageUrl()).isEqualTo("chat-image.jpg");
        verify(pushNotificationService).send(
                opponent,
                AlarmSettingType.NOTIFICATION,
                "김중민",
                "사진을 보냈습니다.",
                "knockinrn://chat/10"
        );
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("이미지 메시지 URL과 일치하는 채팅방 이미지 파일이 없으면 실패한다")
    void sendImageMessageRejectsUnknownImageUrl() {
        // Given
        Long chatId = 10L;
        Long senderId = 1L;
        Member member = member(senderId);
        Member opponent = member(2L);
        ChattingRoom chattingRoom = chattingRoom();
        ChatRoomMember roomMember = activeRoomMember(member, chattingRoom);
        ChatMessageDto.Request request = imageMessageRequest("unknown.jpg");
        when(chatRoomMemberRepository.findActiveMemberByRoomIdAndMemberId(chatId, senderId))
                .thenReturn(Optional.of(roomMember));
        when(chatRoomMemberRepository.findPartnerMember(roomMember, chatId)).thenReturn(opponent);
        when(chattingRoomRepository.findById(chatId)).thenReturn(Optional.of(chattingRoom));
        when(fileService.findBySavedFileNameAndType("unknown.jpg", FileType.CHAT_ROOM_IMAGE))
                .thenThrow(new BusinessException(FileErrorCode.FILE_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> chatService.sendUserMessage(chatId, request, senderId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FILE_NOT_FOUND));
        ArgumentCaptor<ChatRoomMessage> messageCaptor = ArgumentCaptor.forClass(ChatRoomMessage.class);
        verify(chatRoomMessageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getMember()).isSameAs(member);
        assertThat(messageCaptor.getValue().getChattingRoom()).isSameAs(chattingRoom);
        assertThat(messageCaptor.getValue().getType()).isEqualTo(MessageType.IMAGE);
        verifyNoInteractions(chatRoomFileRepository, publisher, messagingTemplate);
    }

    @Test
    @DisplayName("차단 관계에서는 기존 채팅방에서도 메시지를 전송할 수 없다")
    void sendUserMessageRejectsBlockedMembers() {
        // Given
        Long chatId = 10L;
        Long senderId = 1L;
        Member sender = member(senderId);
        Member opponent = member(2L);
        ChatRoomMember roomMember = activeRoomMember(sender, chattingRoom());
        ChatMessageDto.Request request = textMessageRequest();
        when(chatRoomMemberRepository.findActiveMemberByRoomIdAndMemberId(chatId, senderId))
                .thenReturn(Optional.of(roomMember));
        when(chatRoomMemberRepository.findPartnerMember(roomMember, chatId)).thenReturn(opponent);
        when(blockService.isBlockedBetween(senderId, opponent.getId())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> chatService.sendUserMessage(chatId, request, senderId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ChattingErrorCode.MESSAGE_BLOCKED));
        verifyNoInteractions(
                chattingRoomRepository,
                chatRoomMessageRepository,
                chatRoomFileRepository,
                publisher,
                messagingTemplate
        );
    }

    @Test
    @DisplayName("수신자가 비활성 상태이면 기존 채팅방에서도 메시지를 전송할 수 없다")
    void sendUserMessageRejectsInactiveReceiver() {
        // Given
        Long chatId = 10L;
        Long senderId = 1L;
        Member sender = member(senderId);
        Member receiver = member(2L);
        ChatRoomMember roomMember = activeRoomMember(sender, chattingRoom());
        ChatMessageDto.Request request = textMessageRequest();
        when(chatRoomMemberRepository.findActiveMemberByRoomIdAndMemberId(chatId, senderId))
                .thenReturn(Optional.of(roomMember));
        when(chatRoomMemberRepository.findPartnerMember(roomMember, chatId)).thenReturn(receiver);
        when(stateRepository.findByMemberId(receiver.getId()))
                .thenReturn(List.of(State.builder().states(MemberState.INACTIVE).member(receiver).build()));

        // When & Then
        assertThatThrownBy(() -> chatService.sendUserMessage(chatId, request, senderId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.NOT_ACTIVE_MEMBER));
        verifyNoInteractions(
                chattingRoomRepository,
                chatRoomMessageRepository,
                chatRoomFileRepository,
                publisher,
                pushNotificationService,
                messagingTemplate
        );
    }

    @Test
    @DisplayName("채팅방에 참여 중인 멤버가 아니면 메시지를 저장하지 않는다")
    void sendUserMessageRejectsMemberWhoIsNotActiveRoomMember() {
        // Given
        Long chatId = 10L;
        Long senderId = 1L;
        ChatMessageDto.Request request = textMessageRequest();
        when(chatRoomMemberRepository.findActiveMemberByRoomIdAndMemberId(chatId, senderId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatService.sendUserMessage(chatId, request, senderId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ChattingErrorCode.ROOM_MEMBER_NOT_FOUND));
        verifyNoInteractions(chatRoomMessageRepository, chatRoomFileRepository, publisher, messagingTemplate);
    }

    @Test
    @DisplayName("텍스트 메시지 본문이 없으면 메시지를 저장하지 않는다")
    void sendMessageRejectsTextMessageWithoutUserMessage() {
        // Given
        ChatMessageDto.Request request = new ChatMessageDto.Request();
        request.setClientMessageId("client-message-id");
        request.setType(MessageType.TEXT);

        // When & Then
        assertThatThrownBy(() -> chatService.sendUserMessage(10L, request, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ChattingErrorCode.MESSAGE_PAYLOAD_INVALID));
        verifyNoInteractions(chatRoomMemberRepository, chatRoomMessageRepository, messagingTemplate);
    }

    @Test
    @DisplayName("이미지 메시지 URL이 없으면 메시지를 저장하지 않는다")
    void sendMessageRejectsImageUserMessageWithoutImageUrl() {
        // Given
        ChatMessageDto.Request request = new ChatMessageDto.Request();
        request.setClientMessageId("client-message-id");
        request.setType(MessageType.IMAGE);

        // When & Then
        assertThatThrownBy(() -> chatService.sendUserMessage(10L, request, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ChattingErrorCode.MESSAGE_PAYLOAD_INVALID));
        verifyNoInteractions(chatRoomMemberRepository, chatRoomMessageRepository, messagingTemplate);
    }

    @Test
    @DisplayName("메시지 전송 이벤트가 커밋된 후 채팅 메시지를 구독 채널로 발행한다")
    void handleMessageSendPublishesChatMessageToRoomDestination() {
        // Given
        Long chatId = 10L;
        Long senderId = 1L;
        ChatRoomMessageEvent event = ChatRoomMessageEvent.builder()
                .chatRoomId(chatId)
                .senderId(senderId)
                .clientMessageId("client-message-id")
                .messageType(MessageType.TEXT)
                .message("안녕하세요")
                .build();

        // When
        chatService.handleMessageSend(event);

        // Then
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/sub/chats/10"), payloadCaptor.capture());

        ChatSocketResponse<ChatMessageDto.Response> response = (ChatSocketResponse<ChatMessageDto.Response>) payloadCaptor.getValue();
        assertThat(response.getEventType()).isEqualTo(EventType.USER_MESSAGE);
        assertThat(response.getChatRoomId()).isEqualTo(chatId);
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getPayload().getClientMessageId()).isEqualTo("client-message-id");
        assertThat(response.getPayload().getSenderId()).isEqualTo(senderId);
        assertThat(response.getPayload().getType()).isEqualTo(MessageType.TEXT);
        assertThat(response.getPayload().getContents()).isEqualTo("안녕하세요");
    }

    @Test
    @DisplayName("활성 채팅방 멤버가 채팅방을 나가면 나간 상태로 변경하고 퇴장 이벤트를 발행 요청한다")
    void leaveChatRoomMarksMemberAsLeftAndPublishesUserLeftEvent() {
        // Given
        Long chatRoomId = 10L;
        Long memberId = 1L;
        ChattingRoom chattingRoom = chattingRoom();
        ChatRoomMember roomMember = ChatRoomMember.builder()
                .isLeft(false)
                .build();
        when(chatRoomMemberRepository.findActiveMemberByRoomIdAndMemberId(chatRoomId, memberId))
                .thenReturn(Optional.of(roomMember));
        when(chattingRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chattingRoom));
        when(chatRoomMessageRepository.save(any(ChatRoomMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(roommateMatchingRequiredRepository.findLatest(chatRoomId)).thenReturn(Optional.empty());

        // When
        ChatRoomDto.Response result = chatService.leaveChatRoom(memberId, chatRoomId);

        // Then
        assertThat(roomMember.getIsLeft()).isTrue();
        assertThat(result.getUpdatedAt()).isNotNull();
        ArgumentCaptor<ChatRoomMessage> messageCaptor = ArgumentCaptor.forClass(ChatRoomMessage.class);
        verify(chatRoomMessageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getMember()).isNull();
        assertThat(messageCaptor.getValue().getChattingRoom()).isSameAs(chattingRoom);
        assertThat(messageCaptor.getValue().getType()).isEqualTo(MessageType.LEFT_ROOM);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        verifyNoInteractions(messagingTemplate);

        ChatRoomLeftEvent event = (ChatRoomLeftEvent) eventCaptor.getValue();
        assertThat(event.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(event.leftAt()).isEqualTo(result.getUpdatedAt());
        assertThat(event.message()).isEqualTo("상대방이 나갔습니다.");
    }

    @Test
    @DisplayName("진행 중인 룸메이트 확정 제안이 있을 때 상대방이 채팅방을 나가면 제안을 자동 취소한다")
    void leaveChatRoomCancelsPendingRoommateRequest() {
        // Given
        Long chatRoomId = 10L;
        Long memberId = 1L;
        ChattingRoom chattingRoom = chattingRoom();
        ChatRoomMember roomMember = ChatRoomMember.builder()
                .isLeft(false)
                .build();
        RoommateMatchingRequired pendingRequest = RoommateMatchingRequired.builder()
                .status(RoommateRequiredStatus.PENDING)
                .build();
        when(chatRoomMemberRepository.findActiveMemberByRoomIdAndMemberId(chatRoomId, memberId))
                .thenReturn(Optional.of(roomMember));
        when(chattingRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chattingRoom));
        when(chatRoomMessageRepository.save(any(ChatRoomMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(roommateMatchingRequiredRepository.findLatest(chatRoomId))
                .thenReturn(Optional.of(pendingRequest));

        // When
        chatService.leaveChatRoom(memberId, chatRoomId);

        // Then
        assertThat(roomMember.getIsLeft()).isTrue();
        assertThat(pendingRequest.getStatus()).isEqualTo(RoommateRequiredStatus.CANCELED);
        verify(roommateMatchingRequiredRepository).findLatest(chatRoomId);
    }

    @Test
    @DisplayName("이미 종료된 룸메이트 확정 제안은 상대방이 채팅방을 나가도 상태를 변경하지 않는다")
    void leaveChatRoomKeepsCompletedRoommateRequestStatus() {
        // Given
        Long chatRoomId = 10L;
        Long memberId = 1L;
        ChattingRoom chattingRoom = chattingRoom();
        ChatRoomMember roomMember = ChatRoomMember.builder()
                .isLeft(false)
                .build();
        RoommateMatchingRequired rejectedRequest = RoommateMatchingRequired.builder()
                .status(RoommateRequiredStatus.REJECTED)
                .build();
        when(chatRoomMemberRepository.findActiveMemberByRoomIdAndMemberId(chatRoomId, memberId))
                .thenReturn(Optional.of(roomMember));
        when(chattingRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chattingRoom));
        when(chatRoomMessageRepository.save(any(ChatRoomMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(roommateMatchingRequiredRepository.findLatest(chatRoomId))
                .thenReturn(Optional.of(rejectedRequest));

        // When
        chatService.leaveChatRoom(memberId, chatRoomId);

        // Then
        assertThat(roomMember.getIsLeft()).isTrue();
        assertThat(rejectedRequest.getStatus()).isEqualTo(RoommateRequiredStatus.REJECTED);
        verify(roommateMatchingRequiredRepository).findLatest(chatRoomId);
    }

    @Test
    @DisplayName("채팅방 나가기 이벤트가 커밋된 후 퇴장 이벤트를 구독 채널로 발행한다")
    void handleChatRoomLeftPublishesUserLeftEventToRoomDestination() {
        // Given
        Long chatRoomId = 10L;
        LocalDateTime leftAt = LocalDateTime.of(2026, 6, 19, 21, 50);
        ChatRoomLeftEvent event = new ChatRoomLeftEvent(chatRoomId, leftAt, "상대방이 나갔습니다.");

        // When
        chatService.handleChatRoomLeft(event);

        // Then
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/sub/chats/10"), payloadCaptor.capture());

        ChatSocketResponse<ChatMessageDto.Response> response = (ChatSocketResponse<ChatMessageDto.Response>) payloadCaptor.getValue();
        assertThat(response.getEventType()).isEqualTo(EventType.SYSTEM_MESSAGE);
        assertThat(response.getChatRoomId()).isEqualTo(chatRoomId);
        assertThat(response.getCreatedAt()).isEqualTo(leftAt);
        assertThat(response.getPayload().getSenderId()).isNull();
        assertThat(response.getPayload().getType()).isEqualTo(MessageType.LEFT_ROOM);
        assertThat(response.getPayload().getContents()).isEqualTo("상대방이 나갔습니다.");
    }

    @Test
    @DisplayName("활성 채팅방 멤버가 아니면 채팅방 나가기를 거부하고 퇴장 이벤트를 발행하지 않는다")
    void leaveChatRoomRejectsMemberWhoIsNotActiveRoomMember() {
        // Given
        Long chatRoomId = 10L;
        Long memberId = 1L;
        when(chatRoomMemberRepository.findActiveMemberByRoomIdAndMemberId(chatRoomId, memberId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatService.leaveChatRoom(memberId, chatRoomId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ChattingErrorCode.ROOM_MEMBER_NOT_FOUND));
        verifyNoInteractions(publisher, messagingTemplate);
    }

    private ChatRoomListRow chatRoomRow(
            Long chatRoomId,
            Long opponentMemberId,
            String memberName,
            String memberProfileImageUrl,
            LocalDateTime createdAt
    ) {
        return new ChatRoomListRow(
                chatRoomId,
                opponentMemberId,
                memberName,
                memberProfileImageUrl,
                createdAt,
                RoommateRequiredStatus.PENDING,
                false,
                "마지막 메시지",
                createdAt.plusMinutes(1)
        );
    }

    private ChatMessageDto.Request textMessageRequest() {
        ChatMessageDto.Request request = new ChatMessageDto.Request();
        request.setClientMessageId("client-message-id");
        request.setType(MessageType.TEXT);
        request.setMessage("안녕하세요");
        return request;
    }

    private ChatMessageDto.Request imageMessageRequest(String imageUrl) {
        ChatMessageDto.Request request = new ChatMessageDto.Request();
        request.setClientMessageId("client-message-id");
        request.setType(MessageType.IMAGE);
        request.setImageUrl(imageUrl);
        return request;
    }

    private ChatRoomCreateDto.Request chatRoomCreateRequest(Long requesteeId, Long boardId, String contents) {
        ChatRoomCreateDto.ChatMessage chatMessage = new ChatRoomCreateDto.ChatMessage();
        chatMessage.setContents(contents);

        ChatRoomCreateDto.Request request = new ChatRoomCreateDto.Request();
        request.setRequesteeId(requesteeId);
        request.setBoardId(boardId);
        request.setChatMessage(chatMessage);
        return request;
    }

    private ChatRoomMember activeRoomMember(Member member, ChattingRoom chattingRoom) {
        return ChatRoomMember.builder()
                .member(member)
                .chattingRoom(chattingRoom)
                .isLeft(false)
                .build();
    }

    private Member member() {
        return Member.builder().build();
    }

    private Member member(Long id) {
        return Member.builder().id(id).build();
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

    private ChattingRoom chattingRoom() {
        ChattingRequired chattingRequired = ChattingRequired.builder().id(100L).build();
        return ChattingRoom.builder().chattingRequired(chattingRequired).build();
    }

    private ChattingRoom persistedChattingRoom(ChattingRoom chattingRoom, Long id) {
        ReflectionTestUtils.setField(chattingRoom, "id", id);
        return chattingRoom;
    }

    private ChatRoomMessage persistedMessage(ChatRoomMessage message, LocalDateTime createdAt) {
        ReflectionTestUtils.setField(message, "createdAt", createdAt);
        return message;
    }

    private File chatImage(String savedFileName) {
        return File.builder()
                .type(FileType.CHAT_ROOM_IMAGE)
                .originalFileName(savedFileName)
                .savedFileName(savedFileName)
                .fileExt("jpg")
                .isDeleted(false)
                .build();
    }

    private MultipartFile multipartFile(boolean empty) {
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.isEmpty()).thenReturn(empty);
        return multipartFile;
    }
}
