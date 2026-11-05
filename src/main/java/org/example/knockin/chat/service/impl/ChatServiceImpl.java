package org.example.knockin.chat.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.example.knockin.alarm.service.impl.PushNotificationServiceImpl;
import org.example.knockin.board.service.impl.RoommateBoardServiceImpl;
import org.example.knockin.chat.dto.ChatMessageDto;
import org.example.knockin.chat.dto.ChatRoomCreateDto;
import org.example.knockin.chat.dto.ChatRoomDetailDto;
import org.example.knockin.chat.dto.ChatRoomDto;
import org.example.knockin.chat.dto.ChatRoomDto.Response;
import org.example.knockin.chat.dto.ChatRoomImageDto;
import org.example.knockin.chat.dto.ChatRoomListDto;
import org.example.knockin.chat.dto.ChatRoomLeftEvent;
import org.example.knockin.chat.dto.ChatRoomMessageEvent;
import org.example.knockin.chat.dto.ChatSocketResponse;
import org.example.knockin.chat.dto.EventType;
import org.example.knockin.chat.dto.MessageType;
import org.example.knockin.mate.dto.RoommateRequestDto.RoommateMatchingRequiredInfo;
import org.example.knockin.alarm.entity.AlarmSettingType;
import org.example.knockin.verification.entity.AuthenticationType;
import org.example.knockin.chat.entity.ChatRoomMember;
import org.example.knockin.chat.entity.ChatRoomMessage;
import org.example.knockin.chat.entity.ChattingRequired;
import org.example.knockin.chat.entity.ChattingRoom;
import org.example.knockin.chat.entity.ChattingScore;
import org.example.knockin.meta.entity.File;
import org.example.knockin.meta.entity.FileType;
import org.example.knockin.member.entity.Member;
import org.example.knockin.mate.entity.RoommateMatchingRequired;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.ChattingErrorCode;
import org.example.knockin.global.exception.FileErrorCode;
import org.example.knockin.global.entity.ChatAlarmTemplate;
import org.example.knockin.global.util.DateUtils;
import org.example.knockin.member.repository.row.ChattingRoomBasicInfoRow;
import org.example.knockin.verification.repository.row.MemberAuthenticationRow;
import org.example.knockin.chat.repository.row.ChatRoomListRow;
import org.example.knockin.chat.repository.row.ChatRoomUnreadCountRow;
import org.example.knockin.meta.service.FileService;
import org.example.knockin.util.service.RoommateScoreService;
import org.example.knockin.verification.service.impl.AuthenticationServiceImpl;
import org.example.knockin.member.service.impl.BasicInformationServiceImpl;
import org.example.knockin.member.service.impl.BlockServiceImpl;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.example.knockin.mate.service.impl.MyRoomMateServiceImpl;
import org.example.knockin.mate.service.impl.RoommateMatchingRequiredServiceImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl {
    private static final String ROOM_LEAVE_MESSAGE_CONTENTS = "상대방이 나갔습니다.";
    private static final String IMAGE_MESSAGE_CONTENTS = "사진을 보냈습니다.";

    private final ChattingRoomServiceImpl chattingRoomService;
    private final ChatRoomMemberServiceImpl chatRoomMemberService;
    private final SimpMessageSendingOperations messagingTemplate;
    private final ApplicationEventPublisher publisher;
    private final ChatRoomMessageServiceImpl chatRoomMessageService;
    private final FileService fileService;
    private final ChatRoomFileServiceImpl chatRoomFileService;
    private final BasicInformationServiceImpl basicInformationService;
    private final RoommateMatchingRequiredServiceImpl roommateMatchingRequiredService;
    private final RoommateBoardServiceImpl roommateBoardService;
    private final ChattingRequiredServiceImpl chattingRequiredService;
    private final MemberServiceImpl memberService;
    private final RoommateScoreService javaRoommateScoreV2Service;
    private final ChattingScoreServiceImpl chattingScoreService;
    private final BlockServiceImpl blockService;
    private final PushNotificationServiceImpl pushNotificationService;
    private final AuthenticationServiceImpl authenticationService;
    private final MyRoomMateServiceImpl myRoomMateService;
    @Value("${policy.chat.room-limit-per-member}")
    private long chatRoomLimitPerMember;

    @Transactional(readOnly = true)
    public List<ChatRoomListDto.Response> getChatRoomList(Long memberId) {
        List<ChatRoomListRow> rows = chattingRoomService.findListRowsByMemberId(memberId);
        if (rows.isEmpty()) return List.of();

        List<Long> chatRoomIds = rows.stream().map(ChatRoomListRow::chatRoomId).toList();
        List<Long> opponentMemberIds = rows.stream().map(ChatRoomListRow::opponentMemberId).distinct().toList();

        Map<Long, Long> unreadCountsByChatRoomId = chatRoomMessageService.findUnreadMessageCounts(memberId, chatRoomIds).stream()
                .collect(Collectors.toMap(
                        ChatRoomUnreadCountRow::chatRoomId,
                        ChatRoomUnreadCountRow::messageCount
                ));
        Map<Long, List<AuthenticationType>> authenticationTypesByMemberId = authenticationService.findAcceptedByMemberIds(opponentMemberIds).stream()
                .collect(Collectors.groupingBy(
                        MemberAuthenticationRow::memberId,
                        Collectors.mapping(MemberAuthenticationRow::type, Collectors.toList())
                ));

        return rows.stream()
                .map(row -> toChatRoomListResponse(
                        row,
                        unreadCountsByChatRoomId.getOrDefault(row.chatRoomId(), 0L),
                        authenticationTypesByMemberId.getOrDefault(row.opponentMemberId(), List.of())
                ))
                .toList();
    }

    private ChatRoomListDto.Response toChatRoomListResponse(ChatRoomListRow row, Long unreadMessageCount, List<AuthenticationType> authenticationTypes) {
        return ChatRoomListDto.Response.builder()
                .chatRoomId(row.chatRoomId())
                .memberName(row.memberName())
                .memberProfileImageUrl(row.memberProfileImageUrl())
                .createdAt(row.createdAt())
                .roommateStatus(row.roommateStatus())
                .isRoommate(row.isRoommate())
                .authenticationTypes(authenticationTypes)
                .lastMessage(row.lastMessage())
                .messageCount(Math.toIntExact(unreadMessageCount))
                .lastMessageAt(row.lastMessageAt())
                .build();
    }

    @Transactional
    public ChatRoomImageDto.Response uploadImage(Long chatRoomId, Long memberId, MultipartFile multipartFile) {
        validateImageFile(multipartFile);
        validateCanSendMessage(chatRoomId, memberId);

        try {
            File savedFile = fileService.save(multipartFile, FileType.CHAT_ROOM_IMAGE);
            return new ChatRoomImageDto.Response(savedFile.getSavedFileName());
        } catch (IOException e) {
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Transactional
    public ChatRoomDto.Response leaveChatRoom(Long memberId, Long chatRoomId) {
        if(myRoomMateService.findByMyRoommate(memberId) != null) throw new BusinessException(ChattingErrorCode.CANNOT_LEAVE_ROOMMATE_CHAT);

        ChatRoomMember roomMember = chatRoomMemberService.findActiveMemberByRoomIdAndMemberId(chatRoomId, memberId);
        roomMember.left();

        ChattingRoom chattingRoom = chattingRoomService.findByIdOrThrow(chatRoomId);
        chatRoomMessageService.save(ROOM_LEAVE_MESSAGE_CONTENTS, null, chattingRoom, MessageType.LEFT_ROOM);

        LocalDateTime now = LocalDateTime.now();
        publisher.publishEvent(new ChatRoomLeftEvent(chatRoomId, now, ROOM_LEAVE_MESSAGE_CONTENTS));

        roommateMatchingRequiredService.findLatest(chatRoomId)
                .filter(RoommateMatchingRequired::isPending)
                .ifPresent(RoommateMatchingRequired::cancel);

        return new Response(now);
    }

    @Transactional
    public void sendUserMessage(Long chatRoomId, ChatMessageDto.Request request, Long senderId) {
        validateMessageRequest(request);
        List<ChatRoomMember> chatRoomMemberList = chatRoomMemberService.findChatRoomMemberById(chatRoomId);
        if (chatRoomMemberList.stream().anyMatch(ChatRoomMember::getIsLeft)) {
            throw new BusinessException(ChattingErrorCode.CANNOT_SEND_TO_LEFT_MEMBER);
        }
        ChatRoomMember senderChatRoomMember = chatRoomMemberService.findActiveMemberByRoomIdAndMemberId(chatRoomId, senderId);
        Member sender = senderChatRoomMember.getMember();
        Member receiver = chatRoomMemberService.findPartnerMember(senderChatRoomMember, chatRoomId);
        validateNotBlocked(senderId, receiver.getId());
        memberService.validateMemberState(receiver.getId());

        ChattingRoom chattingRoom = chattingRoomService.findByIdOrThrow(chatRoomId);
        MessageType type = request.getType();
        String notificationContents;

        switch (type) {
            case TEXT -> {
                chatRoomMessageService.save(request.getMessage(), sender, chattingRoom, type);
                publishMessageEvent(chatRoomId, senderId, request);
                notificationContents = request.getMessage();
            }
            case IMAGE -> {
                ChatRoomMessage chatRoomMessage = chatRoomMessageService.save(IMAGE_MESSAGE_CONTENTS, sender, chattingRoom, type);
                File file = fileService.findBySavedFileNameAndType(request.getImageUrl(), FileType.CHAT_ROOM_IMAGE);
                chatRoomFileService.save(file, chatRoomMessage);
                publishMessageEvent(chatRoomId, senderId, request);
                notificationContents = IMAGE_MESSAGE_CONTENTS;
            }
            default -> throw new BusinessException(ChattingErrorCode.MESSAGE_PAYLOAD_INVALID);
        }

        sendPushNotification(receiver, sender, chatRoomId, notificationContents);
    }

    private void sendPushNotification(Member receiver, Member sender, Long chatRoomId, String messageContents) {
        String senderName = basicInformationService.findLatestBasicInformation(sender).getName();
        ChatAlarmTemplate template = ChatAlarmTemplate.MESSAGE;
        String title = template.formatTitle(senderName);
        String contents = template.formatContents(messageContents);
        String deepLink = template.formatDeepLink(chatRoomId);

        pushNotificationService.send(receiver, AlarmSettingType.NOTIFICATION, title, contents, deepLink);
    }

    private void publishMessageEvent(Long chatRoomId, Long senderId, ChatMessageDto.Request request) {
        ChatRoomMessageEvent event = ChatRoomMessageEvent.builder()
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .clientMessageId(request.getClientMessageId())
                .messageType(request.getType())
                .message(request.getMessage())
                .imageUrl(request.getImageUrl())
                .build();

        publisher.publishEvent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatRoomLeft(ChatRoomLeftEvent event) {
        ChatSocketResponse<ChatMessageDto.Response> response = ChatSocketResponse.of(
                EventType.SYSTEM_MESSAGE,
                event.chatRoomId(),
                ChatMessageDto.Response.userLeft(event),
                event.leftAt()
        );
        messagingTemplate.convertAndSend("/sub/chats/" + event.chatRoomId(), response);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMessageSend(ChatRoomMessageEvent event) {
        ChatSocketResponse<ChatMessageDto.Response> response = ChatSocketResponse.of(
                EventType.USER_MESSAGE,
                event.chatRoomId(),
                ChatMessageDto.Response.chatMessage(event)
        );
        messagingTemplate.convertAndSend("/sub/chats/" + event.chatRoomId(), response);
    }

    private void validateMessageRequest(ChatMessageDto.Request request) {
        if (request == null
                || !StringUtils.hasText(request.getClientMessageId())
                || request.getType() == null) {
            throw new BusinessException(ChattingErrorCode.MESSAGE_PAYLOAD_INVALID);
        }

        switch (request.getType()) {
            case TEXT -> validateTextMessage(request);
            case IMAGE -> validateImageMessage(request);
        }
    }

    @Transactional
    public ChatRoomDetailDto.Response getChatRoomDetail(Long chatRoomId, Long memberId) {
        ChattingRoom chattingRoom = chattingRoomService.findByIdOrThrow(chatRoomId);
        ChatRoomMember chatRoomMember = chatRoomMemberService.findActiveMemberByRoomIdAndMemberId(chatRoomId, memberId);
        chatRoomMessageService.markUnreadMessagesAsRead(chatRoomId, memberId);

        Member opponentMember = chatRoomMemberService.findPartnerMember(chatRoomMember, chatRoomId);
        ChatRoomDetailDto.ProfileInfo opponentProfile = getOpponentProfileInfo(chattingRoom, chatRoomMember, opponentMember);
        List<ChatRoomDetailDto.ChatMessage> messages = chatRoomMessageService.findChatMessageDto(chatRoomId);
        List<RoommateMatchingRequiredInfo> matchingRequiredList = roommateMatchingRequiredService.findRequiredDto(chattingRoom);
        boolean blocked = blockService.isBlockedBetween(memberId, opponentMember.getId());
        boolean opponentHasRoommate = myRoomMateService.isExistRoomMate(opponentMember);

        return ChatRoomDetailDto.Response.builder()
                .opponentProfile(opponentProfile)
                .messages(messages)
                .matchingRequiredList(matchingRequiredList)
                .blocked(blocked)
                .opponentHasRoommate(opponentHasRoommate)
                .build();
    }

    private ChatRoomDetailDto.ProfileInfo getOpponentProfileInfo(ChattingRoom chattingRoom, ChatRoomMember me, Member opponentMember) {
        ChattingRoomBasicInfoRow row = basicInformationService.findChattingRoomBasicInfoRowByMemberId(opponentMember.getId());
        Long memberId = me.getMember().getId();
        Integer score = chattingScoreService
                .findByChattingRequiredIdAndMemberId(chattingRoom.getChattingRequired().getId(), memberId)
                .map(ChattingScore::getScore)
                .orElseGet(() -> javaRoommateScoreV2Service.calculateSimpleScore(memberId, opponentMember.getId()));

        return ChatRoomDetailDto.ProfileInfo.builder()
                .id(row.memberId())
                .name(row.name())
                .age(DateUtils.calculateAge(row.birth()))
                .gender(row.gender())
                .memberProfileImageUrl(row.profileImageUrl())
                .score(score)
                .build();
    }

    @Transactional
    public ChatRoomCreateDto.Response createChattingRoom(Long requesterId, ChatRoomCreateDto.Request request) {
        Member requester = memberService.findByIdOrThrow(requesterId);
        Member requestee = memberService.findByIdOrThrow(request.getRequesteeId());

        validateNotBlocked(requesterId, request.getRequesteeId());
        validateActiveRoomDoesNotExist(requesterId, request.getRequesteeId());
        validateChatRoomLimit(requesterId, request.getRequesteeId());

        ChattingRequired chattingRequired = chattingRequiredService.saveAccepted(requester, requestee, null);
        ChattingRoom chattingRoom = chattingRoomService.save(chattingRequired);
        chatRoomMemberService.saveAll(chattingRoom, List.of(requester, requestee));
        String contents = request.getChatMessage().getContents();
        ChatRoomMessage chatRoomMessage = chatRoomMessageService.save(contents, requester, chattingRoom, MessageType.TEXT);
        chattingScoreService.saveAll(javaRoommateScoreV2Service.createChattingScores(chattingRequired));

        return ChatRoomCreateDto.Response.builder()
                .chatRoomId(chattingRoom.getId())
                .updatedAt(chatRoomMessage.getCreatedAt())
                .build();
    }

    private ChatRoomMember validateCanSendMessage(Long chatRoomId, Long senderId) {
        ChatRoomMember sender =
                chatRoomMemberService.findActiveMemberByRoomIdAndMemberId(chatRoomId, senderId);
        Member opponent = chatRoomMemberService.findPartnerMember(sender, chatRoomId);
        validateNotBlocked(senderId, opponent.getId());
        return sender;
    }

    private void validateNotBlocked(Long firstMemberId, Long secondMemberId) {
        if (blockService.isBlockedBetween(firstMemberId, secondMemberId)) {
            throw new BusinessException(ChattingErrorCode.MESSAGE_BLOCKED);
        }
    }

    private void validateState() {

    }

    private void validateActiveRoomDoesNotExist(Long requesterId, Long requesteeId) {
        if (chattingRoomService.existsActiveRoomBetweenMembers(requesterId, requesteeId)) {
            throw new BusinessException(ChattingErrorCode.ROOM_DUPLICATE);
        }
    }

    private void validateChatRoomLimit(Long requesterId, Long requesteeId) {
        long requesterRoomCount = chattingRoomService.countActiveRoomsByMemberId(requesterId);
        long requesteeRoomCount = chattingRoomService.countActiveRoomsByMemberId(requesteeId);

        if (requesterRoomCount >= chatRoomLimitPerMember || requesteeRoomCount >= chatRoomLimitPerMember) {
            throw new BusinessException(ChattingErrorCode.ROOM_LIMIT_EXCEEDED, chatRoomLimitPerMember);
        }
    }

    private void validateTextMessage(ChatMessageDto.Request request) {
        if (!StringUtils.hasText(request.getMessage())) {
            throw new BusinessException(ChattingErrorCode.MESSAGE_PAYLOAD_INVALID);
        }
    }

    private void validateImageMessage(ChatMessageDto.Request request) {
        if (!StringUtils.hasText(request.getImageUrl())) {
            throw new BusinessException(ChattingErrorCode.MESSAGE_PAYLOAD_INVALID);
        }
    }

    private void validateImageFile(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BusinessException(FileErrorCode.FILE_EMPTY);
        }
    }
}
