package org.example.knockin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.example.knockin.alarm.service.impl.AlarmServiceImpl;
import org.example.knockin.alarm.service.impl.PushNotificationServiceImpl;
import org.example.knockin.chat.dto.ChatSocketResponse;
import org.example.knockin.chat.service.impl.ChatRoomMemberServiceImpl;
import org.example.knockin.chat.service.impl.ChattingScoreServiceImpl;
import org.example.knockin.chat.dto.EventType;
import org.example.knockin.mate.dto.RoommateRequestDto;
import org.example.knockin.mate.dto.RoommateRequestDto.RoommateMatchingRequiredInfo;
import org.example.knockin.mate.dto.RoommateRequestListDto;
import org.example.knockin.alarm.entity.AlarmSettingType;
import org.example.knockin.alarm.entity.AlarmType;
import org.example.knockin.chat.entity.ChatRoomMember;
import org.example.knockin.chat.entity.ChattingRequired;
import org.example.knockin.chat.entity.ChattingRoom;
import org.example.knockin.member.entity.BasicInformation;
import org.example.knockin.member.entity.Gender;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberPrivacy;
import org.example.knockin.member.entity.MemberPrivacyType;
import org.example.knockin.mate.entity.MyRoommate;
import org.example.knockin.mate.entity.RoommateMatchingRequired;
import org.example.knockin.alarm.entity.RoommateMatchingRequiredAlarm;
import org.example.knockin.mate.entity.RoommateRequiredStatus;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.MemberErrorCode;
import org.example.knockin.global.exception.RequiredErrorCode;
import org.example.knockin.chat.repository.ChatRoomMemberRepository;
import org.example.knockin.alarm.service.impl.RoommateMatchingRequiredAlarmServiceImpl;
import org.example.knockin.mate.service.impl.RoommateMatchingRequiredServiceImpl;
import org.example.knockin.mate.service.impl.RoommateRequestServiceImpl;
import org.example.knockin.member.repository.BasicInformationFileRepository;
import org.example.knockin.member.repository.BasicInformationRepository;
import org.example.knockin.mate.repository.MyRoommateRepository;
import org.example.knockin.alarm.repository.RoommateMatchingRequiredAlarmRepository;
import org.example.knockin.mate.repository.RoommateMatchingRequiredRepository;
import org.example.knockin.mate.service.impl.MyRoomMateServiceImpl;
import org.example.knockin.mate.service.impl.MyRoommateScoreServiceImpl;
import org.example.knockin.member.service.impl.BasicInformationServiceImpl;
import org.example.knockin.member.service.impl.MemberPrivacyServiceImpl;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("룸메이트 확정 요청 서비스")
class RoommateRequestServiceImplTest {

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @Mock
    private RoommateMatchingRequiredRepository roommateMatchingRequiredRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private RoommateMatchingRequiredAlarmRepository roommateMatchingRequiredAlarmRepository;

    @Mock
    private AlarmServiceImpl alarmService;

    @Mock
    private BasicInformationRepository basicInformationRepository;

    @Mock
    private MyRoommateRepository myRoommateRepository;

    @Mock
    private MemberPrivacyServiceImpl memberPrivacyService;

    @Mock
    private PushNotificationServiceImpl pushNotificationService;

    @Mock
    private RoommateScoreService roommateScoreService;

    @Mock
    private MyRoommateScoreServiceImpl myRoommateScoreService;

    @Mock
    private ChattingScoreServiceImpl chattingScoreService;

    @InjectMocks
    private RoommateRequestServiceImpl roommateRequestService;

    @BeforeEach
    void setUp() {
        BasicInformationServiceImpl basicInformationService = new BasicInformationServiceImpl(basicInformationRepository, org.mockito.Mockito.mock(
                BasicInformationFileRepository.class));
        MyRoomMateServiceImpl myRoomMateService = new MyRoomMateServiceImpl(
                myRoommateRepository,
                roommateScoreService,
                null,
                null,
                myRoommateScoreService,
                null,
                null,
                chattingScoreService
        );
        RoommateMatchingRequiredAlarmServiceImpl roommateMatchingRequiredAlarmService =
                new RoommateMatchingRequiredAlarmServiceImpl(alarmService);
        ReflectionTestUtils.setField(roommateMatchingRequiredAlarmService, "requestAlarmExpireDays", 7);
        roommateRequestService = new RoommateRequestServiceImpl(
                messagingTemplate,
                new RoommateMatchingRequiredServiceImpl(roommateMatchingRequiredRepository),
                new ChatRoomMemberServiceImpl(chatRoomMemberRepository),
                roommateMatchingRequiredAlarmService,
                myRoomMateService,
                memberPrivacyService,
                pushNotificationService,
                basicInformationService
        );
    }

    @Test
    @DisplayName("대기 중인 요청이 없으면 룸메이트 확정 요청을 저장하고 알림과 채팅방 소켓 이벤트를 발행한다")
    void saveRoommateRequestCreatesRequestAndPublishesAlarmAndSocketEvent() {
        // Given
        Long chatRoomId = 10L;
        Long requesterId = 1L;
        Long requesteeId = 2L;
        Member requester = member(requesterId);
        Member requestee = member(requesteeId);
        ChattingRoom chattingRoom = chattingRoom(chatRoomId);
        ChatRoomMember roomMember = roomMember(requester, chattingRoom);
        BasicInformation requesterBasicInformation = basicInformation(requester, "김중민");

        when(chatRoomMemberRepository.findActiveMemberByRoomIdAndMemberId(chatRoomId, requesterId))
                .thenReturn(Optional.of(roomMember));
        when(chatRoomMemberRepository.findPartnerMember(roomMember, chatRoomId)).thenReturn(requestee);
        when(roommateMatchingRequiredRepository.findLatest(chatRoomId)).thenReturn(Optional.empty());
        when(roommateMatchingRequiredRepository.save(any(RoommateMatchingRequired.class)))
                .thenAnswer(invocation -> persistedRoommateRequest(invocation.getArgument(0), 1000L));
        when(basicInformationRepository.findLatestBasicInformation(requester))
                .thenReturn(Optional.of(requesterBasicInformation));
        RoommateRequestDto.Request request = request(chatRoomId);

        // When
        RoommateRequestDto.Response response = roommateRequestService.saveRoommateRequest(requesterId, request);

        // Then
        RoommateMatchingRequiredInfo info = response.getRoommateMatchingRequiredInfo();
        assertThat(info.getRequiredId()).isEqualTo(1000L);
        assertThat(info.getRequesterMemberId()).isEqualTo(requesterId);
        assertThat(info.getRequesteeMemberId()).isEqualTo(requesteeId);
        assertThat(info.getStatus()).isEqualTo(RoommateRequiredStatus.PENDING);
        assertThat(info.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 6, 23, 10, 0));
        assertThat(info.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 6, 23, 10, 1));

        ArgumentCaptor<RoommateMatchingRequired> requestCaptor = ArgumentCaptor.forClass(RoommateMatchingRequired.class);
        verify(roommateMatchingRequiredRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getRequester()).isSameAs(requester);
        assertThat(requestCaptor.getValue().getRequestee()).isSameAs(requestee);
        assertThat(requestCaptor.getValue().getChattingRoom()).isSameAs(chattingRoom);
        assertThat(requestCaptor.getValue().getStatus()).isEqualTo(RoommateRequiredStatus.PENDING);

        ArgumentCaptor<RoommateMatchingRequiredAlarm> alarmCaptor = ArgumentCaptor.forClass(RoommateMatchingRequiredAlarm.class);
        verify(alarmService).sendToClient(eq(requesteeId), eq(AlarmType.ROOM_MATCHING.name()), alarmCaptor.capture());
        assertThat(alarmCaptor.getValue().getMember()).isSameAs(requestee);
        assertThat(alarmCaptor.getValue().getTitle()).isEqualTo("김중민님의 매칭 요청");
        assertThat(alarmCaptor.getValue().getContents()).isEqualTo("김중민님이 매칭을 요청했어요.");
        assertThat(alarmCaptor.getValue().getType()).isEqualTo(AlarmType.ROOM_MATCHING);
        assertThat(alarmCaptor.getValue().getRoommateMatchingRequired().getId()).isEqualTo(1000L);
        verify(pushNotificationService).send(
                requestee,
                AlarmSettingType.NOTIFICATION,
                "김중민님의 매칭 요청",
                "김중민님이 매칭을 요청했어요.",
                "knockinrn://chat/10"
        );

        ArgumentCaptor<Object> socketCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/sub/chats/10"), socketCaptor.capture());
        ChatSocketResponse<RoommateRequestDto.Response> socketResponse =
                (ChatSocketResponse<RoommateRequestDto.Response>) socketCaptor.getValue();
        assertThat(socketResponse.getEventType()).isEqualTo(EventType.ROOMMATE_REQUEST);
        assertThat(socketResponse.getChatRoomId()).isEqualTo(chatRoomId);
        assertThat(socketResponse.getPayload()).isSameAs(response);
        assertThat(socketResponse.getCreatedAt()).isNotNull();

        InOrder inOrder = inOrder(alarmService, pushNotificationService, messagingTemplate);
        inOrder.verify(alarmService).sendToClient(eq(requesteeId), eq(AlarmType.ROOM_MATCHING.name()), any(RoommateMatchingRequiredAlarm.class));
        inOrder.verify(pushNotificationService).send(
                requestee,
                AlarmSettingType.NOTIFICATION,
                "김중민님의 매칭 요청",
                "김중민님이 매칭을 요청했어요.",
                "knockinrn://chat/10"
        );
        inOrder.verify(messagingTemplate).convertAndSend(eq("/sub/chats/10"), any(ChatSocketResponse.class));
    }

    @Test
    @DisplayName("채팅방에 대기 중인 룸메이트 확정 요청이 있으면 중복 요청을 거부한다")
    void saveRoommateRequestRejectsDuplicatePendingRequest() {
        // Given
        Long chatRoomId = 10L;
        Long requesterId = 1L;
        Member requester = member(requesterId);
        Member requestee = member(2L);
        ChattingRoom chattingRoom = chattingRoom(chatRoomId);
        ChatRoomMember roomMember = roomMember(requester, chattingRoom);
        RoommateMatchingRequired previous = roommateRequest(requestee, requester, chattingRoom, RoommateRequiredStatus.PENDING);

        when(chatRoomMemberRepository.findActiveMemberByRoomIdAndMemberId(chatRoomId, requesterId))
                .thenReturn(Optional.of(roomMember));
        when(chatRoomMemberRepository.findPartnerMember(roomMember, chatRoomId)).thenReturn(requestee);
        when(roommateMatchingRequiredRepository.findLatest(chatRoomId)).thenReturn(Optional.of(previous));

        // When & Then
        assertThatThrownBy(() -> roommateRequestService.saveRoommateRequest(requesterId, request(chatRoomId)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(RequiredErrorCode.ROOMMATE_DUPLICATE));

        verifyNoInteractions(basicInformationRepository, roommateMatchingRequiredAlarmRepository, alarmService,
                pushNotificationService, messagingTemplate);
    }

    @Test
    @DisplayName("종료된 이전 요청이 있으면 현재 요청자와 피요청자 방향으로 새 요청을 저장한다")
    void saveRoommateRequestCreatesNewRequestWithCurrentDirectionAfterFinishedRequest() {
        // Given
        Long chatRoomId = 10L;
        Long requesterId = 2L;
        Long requesteeId = 1L;
        Member previousRequester = member(1L);
        Member requester = member(requesterId);
        Member requestee = member(requesteeId);
        ChattingRoom chattingRoom = chattingRoom(chatRoomId);
        ChatRoomMember roomMember = roomMember(requester, chattingRoom);
        RoommateMatchingRequired previous = roommateRequest(previousRequester, requester, chattingRoom, RoommateRequiredStatus.REJECTED);

        when(chatRoomMemberRepository.findActiveMemberByRoomIdAndMemberId(chatRoomId, requesterId))
                .thenReturn(Optional.of(roomMember));
        when(chatRoomMemberRepository.findPartnerMember(roomMember, chatRoomId)).thenReturn(requestee);
        when(roommateMatchingRequiredRepository.findLatest(chatRoomId)).thenReturn(Optional.of(previous));
        when(roommateMatchingRequiredRepository.save(any(RoommateMatchingRequired.class)))
                .thenAnswer(invocation -> persistedRoommateRequest(invocation.getArgument(0), 1001L));
        when(basicInformationRepository.findLatestBasicInformation(requester))
                .thenReturn(Optional.of(basicInformation(requester, "이수현")));
        // When
        RoommateRequestDto.Response response = roommateRequestService.saveRoommateRequest(requesterId, request(chatRoomId));

        // Then
        ArgumentCaptor<RoommateMatchingRequired> requestCaptor = ArgumentCaptor.forClass(RoommateMatchingRequired.class);
        verify(roommateMatchingRequiredRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getRequester()).isSameAs(requester);
        assertThat(requestCaptor.getValue().getRequestee()).isSameAs(requestee);
        assertThat(response.getRoommateMatchingRequiredInfo().getRequesterMemberId()).isEqualTo(requesterId);
        assertThat(response.getRoommateMatchingRequiredInfo().getRequesteeMemberId()).isEqualTo(requesteeId);
    }

    @Test
    @DisplayName("요청자의 기본 정보를 찾지 못하면 알림과 채팅방 소켓 이벤트를 발행하지 않는다")
    void saveRoommateRequestDoesNotPublishSocketWhenRequesterBasicInformationIsMissing() {
        // Given
        Long chatRoomId = 10L;
        Long requesterId = 1L;
        Member requester = member(requesterId);
        Member requestee = member(2L);
        ChattingRoom chattingRoom = chattingRoom(chatRoomId);
        ChatRoomMember roomMember = roomMember(requester, chattingRoom);

        when(chatRoomMemberRepository.findActiveMemberByRoomIdAndMemberId(chatRoomId, requesterId))
                .thenReturn(Optional.of(roomMember));
        when(chatRoomMemberRepository.findPartnerMember(roomMember, chatRoomId)).thenReturn(requestee);
        when(roommateMatchingRequiredRepository.findLatest(chatRoomId)).thenReturn(Optional.empty());
        when(roommateMatchingRequiredRepository.save(any(RoommateMatchingRequired.class)))
                .thenAnswer(invocation -> persistedRoommateRequest(invocation.getArgument(0), 1000L));
        when(basicInformationRepository.findLatestBasicInformation(requester)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> roommateRequestService.saveRoommateRequest(requesterId, request(chatRoomId)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.BASIC_INFO_NOT_FOUND));

        verifyNoInteractions(roommateMatchingRequiredAlarmRepository, alarmService, pushNotificationService,
                messagingTemplate);
    }

    @Test
    @DisplayName("피요청자가 요청을 수락하면 양쪽을 비공개로 변경하고 내 룸메이트와 알림과 채팅방 소켓 이벤트를 저장한다")
    void acceptRequiredChangesStatusAndPublishesSideEffects() {
        // Given
        Long requestId = 1000L;
        Long requesterId = 1L;
        Long requesteeId = 2L;
        Long chatRoomId = 10L;
        Member requester = member(requesterId);
        Member requestee = member(requesteeId);
        ChattingRoom chattingRoom = chattingRoom(chatRoomId);
        RoommateMatchingRequired roommateRequest = persistedRoommateRequest(
                roommateRequest(requester, requestee, chattingRoom, RoommateRequiredStatus.PENDING),
                requestId
        );

        when(roommateMatchingRequiredRepository.findById(requestId)).thenReturn(Optional.of(roommateRequest));
        when(basicInformationRepository.findLatestBasicInformation(requestee))
                .thenReturn(Optional.of(basicInformation(requestee, "이수현")));
        MemberPrivacy requesterPrivacy = MemberPrivacy.builder()
                .type(MemberPrivacyType.PUBLIC)
                .build();
        MemberPrivacy requesteePrivacy = MemberPrivacy.builder()
                .type(MemberPrivacyType.PUBLIC)
                .build();
        when(memberPrivacyService.findByMemberId(requesterId)).thenReturn(List.of(requesterPrivacy));
        when(memberPrivacyService.findByMemberId(requesteeId)).thenReturn(List.of(requesteePrivacy));

        // When
        RoommateRequestDto.Response response = roommateRequestService.acceptRequired(requesteeId, requestId);

        // Then
        RoommateMatchingRequiredInfo info = response.getRoommateMatchingRequiredInfo();
        assertThat(info.getRequiredId()).isEqualTo(requestId);
        assertThat(info.getRequesterMemberId()).isEqualTo(requesterId);
        assertThat(info.getRequesteeMemberId()).isEqualTo(requesteeId);
        assertThat(info.getStatus()).isEqualTo(RoommateRequiredStatus.ACCEPTED);
        assertThat(roommateRequest.getStatus()).isEqualTo(RoommateRequiredStatus.ACCEPTED);

        ArgumentCaptor<MyRoommate> myRoommateCaptor = ArgumentCaptor.forClass(MyRoommate.class);
        verify(myRoommateRepository).save(myRoommateCaptor.capture());
        assertThat(myRoommateCaptor.getValue().getRoommateMatchingRequired()).isSameAs(roommateRequest);
        assertThat(myRoommateCaptor.getValue().getIsDeleted()).isFalse();
        assertThat(requesterPrivacy.getType()).isEqualTo(MemberPrivacyType.PRIVATE);
        assertThat(requesteePrivacy.getType()).isEqualTo(MemberPrivacyType.PRIVATE);
        verify(memberPrivacyService).findByMemberId(requesterId);
        verify(memberPrivacyService).findByMemberId(requesteeId);

        ArgumentCaptor<RoommateMatchingRequiredAlarm> alarmCaptor = ArgumentCaptor.forClass(RoommateMatchingRequiredAlarm.class);
        verify(alarmService).sendToClient(eq(requesterId), eq(AlarmType.ROOM_MATCHING.name()), alarmCaptor.capture());
        assertThat(alarmCaptor.getValue().getMember()).isSameAs(requester);
        assertThat(alarmCaptor.getValue().getTitle()).isEqualTo("매칭 요청이 수락됐어요");
        assertThat(alarmCaptor.getValue().getContents()).isEqualTo("이수현님과의 룸메이트가 이뤄졌어요.");
        assertThat(alarmCaptor.getValue().getRoommateMatchingRequired()).isSameAs(roommateRequest);
        verify(pushNotificationService).send(
                requester,
                AlarmSettingType.NOTIFICATION,
                "매칭 요청이 수락됐어요",
                "이수현님과의 룸메이트가 이뤄졌어요.",
                "knockinrn://chat/10"
        );

        assertSocketResponse(chatRoomId, response);
    }

    @Test
    @DisplayName("요청자나 피요청자에게 현재 룸메이트가 있으면 요청 수락을 거부한다")
    void acceptRequiredRejectsWhenRoommateAlreadyExists() {
        // Given
        Long requestId = 1000L;
        Long requesterId = 1L;
        Long requesteeId = 2L;
        Member requester = member(requesterId);
        Member requestee = member(requesteeId);
        RoommateMatchingRequired roommateRequest = persistedRoommateRequest(
                roommateRequest(requester, requestee, chattingRoom(10L), RoommateRequiredStatus.PENDING),
                requestId
        );

        when(roommateMatchingRequiredRepository.findById(requestId)).thenReturn(Optional.of(roommateRequest));
        when(myRoommateRepository.isExistRoomMate(requester)).thenReturn(false);
        when(myRoommateRepository.isExistRoomMate(requestee)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> roommateRequestService.acceptRequired(requesteeId, requestId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(RequiredErrorCode.ROOMMATE_ALREADY_EXISTS));

        assertThat(roommateRequest.getStatus()).isEqualTo(RoommateRequiredStatus.PENDING);
        verify(myRoommateRepository).isExistRoomMate(requester);
        verify(myRoommateRepository).isExistRoomMate(requestee);
        verify(myRoommateRepository, never()).save(any(MyRoommate.class));
        verifyNoInteractions(memberPrivacyService, basicInformationRepository, roommateMatchingRequiredAlarmRepository,
                alarmService, pushNotificationService, messagingTemplate);
    }

    @Test
    @DisplayName("피요청자가 요청을 거절하면 상태를 거절로 변경하고 요청자에게 알림과 채팅방 소켓 이벤트를 발행한다")
    void rejectRequiredChangesStatusAndPublishesAlarmAndSocketEvent() {
        // Given
        Long requestId = 1000L;
        Long requesterId = 1L;
        Long requesteeId = 2L;
        Long chatRoomId = 10L;
        Member requester = member(requesterId);
        Member requestee = member(requesteeId);
        RoommateMatchingRequired roommateRequest = persistedRoommateRequest(
                roommateRequest(requester, requestee, chattingRoom(chatRoomId), RoommateRequiredStatus.PENDING),
                requestId
        );

        when(roommateMatchingRequiredRepository.findById(requestId)).thenReturn(Optional.of(roommateRequest));
        when(basicInformationRepository.findLatestBasicInformation(requestee))
                .thenReturn(Optional.of(basicInformation(requestee, "이수현")));

        // When
        RoommateRequestDto.Response response = roommateRequestService.rejectRequired(requesteeId, requestId);

        // Then
        assertThat(response.getRoommateMatchingRequiredInfo().getStatus()).isEqualTo(RoommateRequiredStatus.REJECTED);
        assertThat(roommateRequest.getStatus()).isEqualTo(RoommateRequiredStatus.REJECTED);

        ArgumentCaptor<RoommateMatchingRequiredAlarm> alarmCaptor = ArgumentCaptor.forClass(RoommateMatchingRequiredAlarm.class);
        verify(alarmService).sendToClient(eq(requesterId), eq(AlarmType.ROOM_MATCHING.name()), alarmCaptor.capture());
        assertThat(alarmCaptor.getValue().getMember()).isSameAs(requester);
        assertThat(alarmCaptor.getValue().getTitle()).isEqualTo("매칭 요청 거절");
        assertThat(alarmCaptor.getValue().getContents()).isEqualTo("이수현님이 매칭 요청을 거절했어요.");
        assertThat(alarmCaptor.getValue().getRoommateMatchingRequired()).isSameAs(roommateRequest);
        verify(pushNotificationService).send(
                requester,
                AlarmSettingType.NOTIFICATION,
                "매칭 요청 거절",
                "이수현님이 매칭 요청을 거절했어요.",
                "knockinrn://chat/10"
        );

        assertSocketResponse(chatRoomId, response);
        verifyNoInteractions(myRoommateRepository, roommateMatchingRequiredAlarmRepository);
    }

    @Test
    @DisplayName("요청자가 요청을 취소하면 상태를 취소로 변경하고 채팅방 소켓 이벤트만 발행한다")
    void cancelRequiredChangesStatusAndPublishesSocketEvent() {
        // Given
        Long requestId = 1000L;
        Long requesterId = 1L;
        Long requesteeId = 2L;
        Long chatRoomId = 10L;
        RoommateMatchingRequired roommateRequest = persistedRoommateRequest(
                roommateRequest(member(requesterId), member(requesteeId), chattingRoom(chatRoomId), RoommateRequiredStatus.PENDING),
                requestId
        );

        when(roommateMatchingRequiredRepository.findById(requestId)).thenReturn(Optional.of(roommateRequest));

        // When
        RoommateRequestDto.Response response = roommateRequestService.cancelRequired(requesterId, requestId);

        // Then
        assertThat(response.getRoommateMatchingRequiredInfo().getStatus()).isEqualTo(RoommateRequiredStatus.CANCELED);
        assertThat(roommateRequest.getStatus()).isEqualTo(RoommateRequiredStatus.CANCELED);
        assertSocketResponse(chatRoomId, response);
        verifyNoInteractions(myRoommateRepository, basicInformationRepository, roommateMatchingRequiredAlarmRepository,
                alarmService, pushNotificationService);
    }

    @Test
    @DisplayName("요청자가 수락을 시도하면 권한 없음 예외를 던지고 부수 효과를 만들지 않는다")
    void acceptRequiredRejectsRequesterAccess() {
        // Given
        Long requestId = 1000L;
        Long requesterId = 1L;
        RoommateMatchingRequired roommateRequest = persistedRoommateRequest(
                roommateRequest(member(requesterId), member(2L), chattingRoom(10L), RoommateRequiredStatus.PENDING),
                requestId
        );

        when(roommateMatchingRequiredRepository.findById(requestId)).thenReturn(Optional.of(roommateRequest));

        // When & Then
        assertThatThrownBy(() -> roommateRequestService.acceptRequired(requesterId, requestId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(RequiredErrorCode.ROOMMATE_ACCESS_DENIED));
        assertThat(roommateRequest.getStatus()).isEqualTo(RoommateRequiredStatus.PENDING);
        verifyNoInteractions(myRoommateRepository, basicInformationRepository, roommateMatchingRequiredAlarmRepository,
                alarmService, pushNotificationService, messagingTemplate);
    }

    @Test
    @DisplayName("피요청자가 취소를 시도하면 권한 없음 예외를 던지고 부수 효과를 만들지 않는다")
    void cancelRequiredRejectsRequesteeAccess() {
        // Given
        Long requestId = 1000L;
        Long requesteeId = 2L;
        RoommateMatchingRequired roommateRequest = persistedRoommateRequest(
                roommateRequest(member(1L), member(requesteeId), chattingRoom(10L), RoommateRequiredStatus.PENDING),
                requestId
        );

        when(roommateMatchingRequiredRepository.findById(requestId)).thenReturn(Optional.of(roommateRequest));

        // When & Then
        assertThatThrownBy(() -> roommateRequestService.cancelRequired(requesteeId, requestId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(RequiredErrorCode.ROOMMATE_ACCESS_DENIED));
        assertThat(roommateRequest.getStatus()).isEqualTo(RoommateRequiredStatus.PENDING);
        verifyNoInteractions(myRoommateRepository, basicInformationRepository, roommateMatchingRequiredAlarmRepository,
                alarmService, pushNotificationService, messagingTemplate);
    }

    @Test
    @DisplayName("존재하지 않는 요청을 거절하려 하면 조회 실패 예외를 던지고 부수 효과를 만들지 않는다")
    void rejectRequiredRejectsMissingRequest() {
        // Given
        Long requestId = 1000L;
        when(roommateMatchingRequiredRepository.findById(requestId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> roommateRequestService.rejectRequired(2L, requestId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(RequiredErrorCode.ROOMMATE_NOT_FOUND));
        verifyNoInteractions(myRoommateRepository, basicInformationRepository, roommateMatchingRequiredAlarmRepository,
                alarmService, pushNotificationService, messagingTemplate);
    }

    @Test
    @DisplayName("이미 처리된 요청을 수락하려 하면 상태 오류 예외를 던지고 부수 효과를 만들지 않는다")
    void acceptRequiredRejectsInvalidStatus() {
        // Given
        Long requestId = 1000L;
        Long requesteeId = 2L;
        RoommateMatchingRequired roommateRequest = persistedRoommateRequest(
                roommateRequest(member(1L), member(requesteeId), chattingRoom(10L), RoommateRequiredStatus.REJECTED),
                requestId
        );

        when(roommateMatchingRequiredRepository.findById(requestId)).thenReturn(Optional.of(roommateRequest));

        // When & Then
        assertThatThrownBy(() -> roommateRequestService.acceptRequired(requesteeId, requestId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(RequiredErrorCode.ROOMMATE_INVALID_STATUS));
        assertThat(roommateRequest.getStatus()).isEqualTo(RoommateRequiredStatus.REJECTED);
        verifyNoInteractions(myRoommateRepository, basicInformationRepository, roommateMatchingRequiredAlarmRepository,
                alarmService, pushNotificationService, messagingTemplate);
    }

    @Test
    @DisplayName("이미 처리된 요청을 거절하려 하면 상태 오류 예외를 던지고 부수 효과를 만들지 않는다")
    void rejectRequiredRejectsInvalidStatus() {
        // Given
        Long requestId = 1000L;
        Long requesteeId = 2L;
        RoommateMatchingRequired roommateRequest = persistedRoommateRequest(
                roommateRequest(member(1L), member(requesteeId), chattingRoom(10L), RoommateRequiredStatus.ACCEPTED),
                requestId
        );

        when(roommateMatchingRequiredRepository.findById(requestId)).thenReturn(Optional.of(roommateRequest));

        // When & Then
        assertThatThrownBy(() -> roommateRequestService.rejectRequired(requesteeId, requestId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(RequiredErrorCode.ROOMMATE_INVALID_STATUS));
        assertThat(roommateRequest.getStatus()).isEqualTo(RoommateRequiredStatus.ACCEPTED);
        verifyNoInteractions(myRoommateRepository, basicInformationRepository, roommateMatchingRequiredAlarmRepository,
                alarmService, pushNotificationService, messagingTemplate);
    }

    @Test
    @DisplayName("이미 처리된 요청을 취소하려 하면 상태 오류 예외를 던지고 부수 효과를 만들지 않는다")
    void cancelRequiredRejectsInvalidStatus() {
        // Given
        Long requestId = 1000L;
        Long requesterId = 1L;
        RoommateMatchingRequired roommateRequest = persistedRoommateRequest(
                roommateRequest(member(requesterId), member(2L), chattingRoom(10L), RoommateRequiredStatus.EXPIRED),
                requestId
        );

        when(roommateMatchingRequiredRepository.findById(requestId)).thenReturn(Optional.of(roommateRequest));

        // When & Then
        assertThatThrownBy(() -> roommateRequestService.cancelRequired(requesterId, requestId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(RequiredErrorCode.ROOMMATE_INVALID_STATUS));
        assertThat(roommateRequest.getStatus()).isEqualTo(RoommateRequiredStatus.EXPIRED);
        verifyNoInteractions(myRoommateRepository, basicInformationRepository, roommateMatchingRequiredAlarmRepository,
                alarmService, pushNotificationService, messagingTemplate);
    }

    @Test
    @DisplayName("룸메이트 요청 목록을 조회하면 요청 정보를 페이지 응답으로 반환한다")
    void getRequiredListReturnsMappedRequestPage() {
        // Given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 20);
        Member requester = member(memberId);
        Member requestee = member(2L);
        ChattingRoom chattingRoom = chattingRoom(10L);
        RoommateMatchingRequired roommateRequest = persistedRoommateRequest(
                roommateRequest(requester, requestee, chattingRoom, RoommateRequiredStatus.PENDING),
                1000L
        );
        Page<RoommateMatchingRequired> requestPage = new PageImpl<>(List.of(roommateRequest), pageable, 1);

        when(roommateMatchingRequiredRepository.findMyRequiredList(memberId, pageable))
                .thenReturn(requestPage);

        // When
        Page<RoommateRequestListDto.Response> response = roommateRequestService.getRequiredList(memberId, pageable);

        // Then
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getNumber()).isZero();
        assertThat(response.getSize()).isEqualTo(20);

        RoommateRequestListDto.Response content = response.getContent().getFirst();
        assertThat(content.getRequiredId()).isEqualTo(1000L);
        assertThat(content.getRequesterId()).isEqualTo(memberId);
        assertThat(content.getRequesteeId()).isEqualTo(2L);
        assertThat(content.getChatRoomId()).isEqualTo(10L);
        assertThat(content.getStatus()).isEqualTo(RoommateRequiredStatus.PENDING);
        assertThat(content.getCreateAt()).isEqualTo(LocalDateTime.of(2026, 6, 23, 10, 0));

        verify(roommateMatchingRequiredRepository).findMyRequiredList(memberId, pageable);
        verifyNoInteractions(chatRoomMemberRepository, basicInformationRepository, myRoommateRepository,
                roommateMatchingRequiredAlarmRepository, alarmService, pushNotificationService, messagingTemplate);
    }

    private RoommateRequestDto.Request request(Long chatRoomId) {
        RoommateRequestDto.Request request = new RoommateRequestDto.Request();
        request.setChatRoomId(chatRoomId);
        return request;
    }

    private Member member(Long id) {
        return Member.builder()
                .id(id)
                .fcmToken("fcm-token-" + id)
                .build();
    }

    private ChattingRoom chattingRoom(Long id) {
        return ChattingRoom.builder()
                .id(id)
                .chattingRequired(ChattingRequired.builder().id(id).build())
                .build();
    }

    private ChatRoomMember roomMember(Member member, ChattingRoom chattingRoom) {
        return ChatRoomMember.builder()
                .member(member)
                .chattingRoom(chattingRoom)
                .isLeft(false)
                .build();
    }

    private RoommateMatchingRequired roommateRequest(
            Member requester,
            Member requestee,
            ChattingRoom chattingRoom,
            RoommateRequiredStatus status
    ) {
        return RoommateMatchingRequired.builder()
                .requester(requester)
                .requestee(requestee)
                .chattingRoom(chattingRoom)
                .status(status)
                .build();
    }

    private RoommateMatchingRequired persistedRoommateRequest(RoommateMatchingRequired request, Long id) {
        ReflectionTestUtils.setField(request, "id", id);
        ReflectionTestUtils.setField(request, "createdAt", LocalDateTime.of(2026, 6, 23, 10, 0));
        ReflectionTestUtils.setField(request, "updatedAt", LocalDateTime.of(2026, 6, 23, 10, 1));
        return request;
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

    private void assertSocketResponse(Long chatRoomId, RoommateRequestDto.Response response) {
        ArgumentCaptor<Object> socketCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/sub/chats/" + chatRoomId), socketCaptor.capture());
        ChatSocketResponse<RoommateRequestDto.Response> socketResponse =
                (ChatSocketResponse<RoommateRequestDto.Response>) socketCaptor.getValue();
        assertThat(socketResponse.getEventType()).isEqualTo(EventType.ROOMMATE_REQUEST);
        assertThat(socketResponse.getChatRoomId()).isEqualTo(chatRoomId);
        assertThat(socketResponse.getPayload()).isSameAs(response);
        assertThat(socketResponse.getCreatedAt()).isNotNull();
    }
}
