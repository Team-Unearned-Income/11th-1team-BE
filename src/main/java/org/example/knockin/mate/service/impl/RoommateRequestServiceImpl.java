package org.example.knockin.mate.service.impl;

import org.example.knockin.alarm.service.impl.RoommateMatchingRequiredAlarmServiceImpl;

import lombok.RequiredArgsConstructor;
import org.example.knockin.alarm.service.impl.PushNotificationServiceImpl;
import org.example.knockin.chat.dto.ChatSocketResponse;
import org.example.knockin.chat.service.impl.ChatRoomMemberServiceImpl;
import org.example.knockin.chat.dto.EventType;
import org.example.knockin.mate.dto.RoommateRequestDto;
import org.example.knockin.mate.dto.RoommateRequestDto.Response;
import org.example.knockin.mate.dto.RoommateRequestDto.RoommateMatchingRequiredInfo;
import org.example.knockin.mate.dto.RoommateRequestListDto;
import org.example.knockin.alarm.entity.AlarmSettingType;
import org.example.knockin.chat.entity.ChatRoomMember;
import org.example.knockin.chat.entity.ChattingRoom;
import org.example.knockin.member.entity.BasicInformation;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberPrivacy;
import org.example.knockin.member.entity.MemberPrivacyType;
import org.example.knockin.mate.entity.RoommateMatchingRequired;
import org.example.knockin.mate.entity.RoommateRequiredStatus;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.RequiredErrorCode;
import org.example.knockin.global.entity.RoommateRequiredMessageTemplate;
import org.example.knockin.member.service.impl.BasicInformationServiceImpl;
import org.example.knockin.member.service.impl.MemberPrivacyServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoommateRequestServiceImpl {

    private final SimpMessageSendingOperations messagingTemplate;
    private final RoommateMatchingRequiredServiceImpl roommateMatchingRequiredService;
    private final ChatRoomMemberServiceImpl chatRoomMemberService;
    private final RoommateMatchingRequiredAlarmServiceImpl roommateMatchingRequiredAlarmService;
    private final MyRoomMateServiceImpl myRoomMateService;
    private final MemberPrivacyServiceImpl memberPrivacyService;
    private final PushNotificationServiceImpl pushNotificationService;
    private final BasicInformationServiceImpl basicInformationService;

    @Transactional
    public RoommateRequestDto.Response saveRoommateRequest(Long requesterId, RoommateRequestDto.Request request) {
        Long chatRoomId = request.getChatRoomId();
        ChatRoomMember chatRoomMember = chatRoomMemberService.findActiveMemberByRoomIdAndMemberId(chatRoomId, requesterId);
        ChattingRoom chattingRoom = chatRoomMember.getChattingRoom();
        Member requester = chatRoomMember.getMember();
        Long chattingRoomId = chattingRoom.getId();
        Member requestee = chatRoomMemberService.findPartnerMember(chatRoomMember, chattingRoomId);

        RoommateMatchingRequired roommateMatchingRequired = roommateMatchingRequiredService.findLatest(chatRoomId)
                .map(previous -> {
                    if (previous.getStatus().equals(RoommateRequiredStatus.PENDING)) {
                        throw new BusinessException(RequiredErrorCode.ROOMMATE_DUPLICATE);
                    }
                    return roommateMatchingRequiredService.savePending(requester, requestee, chattingRoom);
                })
                .orElseGet(() -> roommateMatchingRequiredService.savePending(requester, requestee, chattingRoom));

        Response response = toDto(roommateMatchingRequired);
        sendAlarms(requestee, requester, roommateMatchingRequired);
        sendRequestMessage(chatRoomId, response);
        return response;
    }

    private void sendAlarms(Member receiver, Member sender, RoommateMatchingRequired required) {
        BasicInformation basicInformation = basicInformationService.findLatestBasicInformation(sender);
        String senderName = basicInformation.getName();

        RoommateRequiredMessageTemplate template = RoommateRequiredMessageTemplate.of(required.getStatus());
        String title = template.formatTitle(senderName);
        String contents = template.formatContents(senderName);
        String deepLink = template.formatDeepLink(required.getChattingRoom().getId());

        roommateMatchingRequiredAlarmService.send(receiver, title, contents, required);
        pushNotificationService.send(receiver, AlarmSettingType.NOTIFICATION, title, contents, deepLink);
    }

    private RoommateRequestDto.Response toDto(RoommateMatchingRequired roommateMatchingRequired) {
        RoommateMatchingRequiredInfo roommateMatchingRequiredInfo = RoommateMatchingRequiredInfo.builder()
                .requiredId(roommateMatchingRequired.getId())
                .requesterMemberId(roommateMatchingRequired.getRequester().getId())
                .requesteeMemberId(roommateMatchingRequired.getRequestee().getId())
                .status(roommateMatchingRequired.getStatus())
                .createdAt(roommateMatchingRequired.getCreatedAt())
                .updatedAt(roommateMatchingRequired.getUpdatedAt())
                .build();

        return RoommateRequestDto.Response.builder()
                .roommateMatchingRequiredInfo(roommateMatchingRequiredInfo)
                .build();
    }

    private void sendRequestMessage(Long chatRoomId, RoommateRequestDto.Response response) {
        ChatSocketResponse<RoommateRequestDto.Response> socketResponse = ChatSocketResponse.of(
                EventType.ROOMMATE_REQUEST,
                chatRoomId,
                response
        );
        messagingTemplate.convertAndSend("/sub/chats/" + chatRoomId, socketResponse);
    }

    @Transactional
    public RoommateRequestDto.Response acceptRequired(Long memberId, Long requestId) {
        RoommateMatchingRequired roommateMatchingRequired = roommateMatchingRequiredService.findByIdOrThrow(requestId);

        if (!roommateMatchingRequired.isRequestee(memberId)) {
            throw new BusinessException(RequiredErrorCode.ROOMMATE_ACCESS_DENIED);
        }

        validateRequired(roommateMatchingRequired);

        Member requester = roommateMatchingRequired.getRequester();
        Member requestee = roommateMatchingRequired.getRequestee();
        if (myRoomMateService.isExistRoomMate(requester) || myRoomMateService.isExistRoomMate(requestee)) {
            throw new BusinessException(RequiredErrorCode.ROOMMATE_ALREADY_EXISTS);
        }

        roommateMatchingRequired.accept();
        myRoomMateService.save(roommateMatchingRequired);

        changePrivacyStateToPrivate(requester, requestee);
        sendAlarms(requester, requestee, roommateMatchingRequired);

        Response response = toDto(roommateMatchingRequired);
        sendRequestMessage(roommateMatchingRequired.getChattingRoom().getId(), response);

        return response;
    }

    private void changePrivacyStateToPrivate(Member requester, Member requestee) {
        MemberPrivacy requesterPrivacy = memberPrivacyService.findByMemberId(requester.getId()).getFirst();
        MemberPrivacy requesteePrivacy = memberPrivacyService.findByMemberId(requestee.getId()).getFirst();

        requesterPrivacy.changeState(MemberPrivacyType.PRIVATE);
        requesteePrivacy.changeState(MemberPrivacyType.PRIVATE);
    }

    @Transactional
    public RoommateRequestDto.Response rejectRequired(Long memberId, Long requestId) {
        RoommateMatchingRequired roommateMatchingRequired = roommateMatchingRequiredService.findByIdOrThrow(requestId);

        if (!roommateMatchingRequired.isRequestee(memberId)) {
            throw new BusinessException(RequiredErrorCode.ROOMMATE_ACCESS_DENIED);
        }

        validateRequired(roommateMatchingRequired);
        roommateMatchingRequired.reject();

        Member requester = roommateMatchingRequired.getRequester();
        Member requestee = roommateMatchingRequired.getRequestee();
        sendAlarms(requester, requestee, roommateMatchingRequired);

        Response response = toDto(roommateMatchingRequired);
        sendRequestMessage(roommateMatchingRequired.getChattingRoom().getId(), response);
        return response;
    }

    @Transactional
    public RoommateRequestDto.Response cancelRequired(Long memberId, Long requestId) {
        RoommateMatchingRequired roommateMatchingRequired = roommateMatchingRequiredService.findByIdOrThrow(requestId);

        if (!roommateMatchingRequired.isRequester(memberId)) {
            throw new BusinessException(RequiredErrorCode.ROOMMATE_ACCESS_DENIED);
        }

        validateRequired(roommateMatchingRequired);
        roommateMatchingRequired.cancel();
        Response response = toDto(roommateMatchingRequired);
        sendRequestMessage(roommateMatchingRequired.getChattingRoom().getId(), response);
        return response;
    }

    public Page<RoommateRequestListDto.Response> getRequiredList(Long memberId, Pageable pageable) {
        return roommateMatchingRequiredService.findMyRequiredList(memberId, pageable).map(this::toListDto);
    }

    private RoommateRequestListDto.Response toListDto(RoommateMatchingRequired roommateMatchingRequired) {
        return RoommateRequestListDto.Response.builder()
                .requiredId(roommateMatchingRequired.getId())
                .requesterId(roommateMatchingRequired.getRequester().getId())
                .requesteeId(roommateMatchingRequired.getRequestee().getId())
                .chatRoomId(roommateMatchingRequired.getChattingRoom().getId())
                .status(roommateMatchingRequired.getStatus())
                .createAt(roommateMatchingRequired.getCreatedAt())
                .build();
    }

    private void validateRequired(RoommateMatchingRequired roommateMatchingRequired) {
        if (!roommateMatchingRequired.isPending()) {
            throw new BusinessException(RequiredErrorCode.ROOMMATE_INVALID_STATUS);
        }
    }
}
