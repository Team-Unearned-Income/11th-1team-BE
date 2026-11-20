package org.example.knockin.mate.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.knockin.chat.event.ChatRoomPartnerQueryEvent;
import org.example.knockin.alarm.event.RoommateRequestAlarmEvent;
import org.example.knockin.chat.event.RoommateRequestMessageEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.example.knockin.global.entity.RoommateRequiredMessageTemplate;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.MemberErrorCode;
import org.example.knockin.global.exception.RequiredErrorCode;
import org.example.knockin.mate.dto.RoommateRequestDto;
import org.example.knockin.mate.dto.RoommateRequestDto.Response;
import org.example.knockin.mate.dto.RoommateRequestDto.RoommateMatchingRequiredInfo;
import org.example.knockin.mate.dto.RoommateRequestListDto;
import org.example.knockin.mate.entity.RoommateMatchingRequired;
import org.example.knockin.mate.entity.RoommateRequiredStatus;
import org.example.knockin.member.entity.BasicInformation;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberPrivacy;
import org.example.knockin.member.entity.MemberPrivacyType;
import org.example.knockin.member.repository.MemberRepository;
import org.example.knockin.member.service.impl.BasicInformationServiceImpl;
import org.example.knockin.member.service.impl.MemberPrivacyServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoommateRequestServiceImpl {

    private final RoommateMatchingRequiredServiceImpl roommateMatchingRequiredService;
    private final MyRoomMateServiceImpl myRoomMateService;
    private final MemberPrivacyServiceImpl memberPrivacyService;
    private final BasicInformationServiceImpl basicInformationService;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Response saveRoommateRequest(Long requesterId, RoommateRequestDto.Request request) {
        Long chatRoomId = request.getChatRoomId();
        ChatRoomPartnerQueryEvent partnerEvent = new ChatRoomPartnerQueryEvent(chatRoomId, requesterId);
        eventPublisher.publishEvent(partnerEvent);
        Long requesteeId = partnerEvent.getResult();
        Member requester = memberRepository.findById(requesterId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
        Member requestee = memberRepository.findById(requesteeId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        RoommateMatchingRequired roommateMatchingRequired = roommateMatchingRequiredService.findLatest(chatRoomId)
                .map(previous -> {
                    if (previous.getStatus().equals(RoommateRequiredStatus.PENDING)) {
                        throw new BusinessException(RequiredErrorCode.ROOMMATE_DUPLICATE);
                    }
                    return roommateMatchingRequiredService.savePending(requester, requestee, chatRoomId);
                })
                .orElseGet(() -> roommateMatchingRequiredService.savePending(requester, requestee, chatRoomId));

        Response response = toDto(roommateMatchingRequired);
        sendAlarms(requestee, requester, roommateMatchingRequired);
        sendRequestMessage(chatRoomId, response);
        return response;
    }

    private void sendAlarms(Member receiver, Member sender, RoommateMatchingRequired required) {
        BasicInformation basicInformation = basicInformationService.findLatestBasicInformation(sender);
        String senderName = basicInformation.getName();

        RoommateRequiredMessageTemplate template = switch (required.getStatus()) {
            case PENDING -> RoommateRequiredMessageTemplate.PENDING;
            case ACCEPTED -> RoommateRequiredMessageTemplate.ACCEPTED;
            case REJECTED -> RoommateRequiredMessageTemplate.REJECTED;
            case CANCELED, EXPIRED -> throw new IllegalArgumentException("지원하지 않는 룸메이트 매칭 상태값입니다.");
        };
        String title = template.formatTitle(senderName);
        String contents = template.formatContents(senderName);
        String deepLink = template.formatDeepLink(required.getChattingRoomId());

        RoommateRequestAlarmEvent event = new RoommateRequestAlarmEvent(receiver.getId(), required.getId(), title, contents, deepLink);
        eventPublisher.publishEvent(event);
        event.requireCompleted();
    }

    private Response toDto(RoommateMatchingRequired roommateMatchingRequired) {
        RoommateMatchingRequiredInfo roommateMatchingRequiredInfo = RoommateMatchingRequiredInfo.builder()
                .requiredId(roommateMatchingRequired.getId())
                .requesterMemberId(roommateMatchingRequired.getRequester().getId())
                .requesteeMemberId(roommateMatchingRequired.getRequestee().getId())
                .status(roommateMatchingRequired.getStatus())
                .createdAt(roommateMatchingRequired.getCreatedAt())
                .updatedAt(roommateMatchingRequired.getUpdatedAt())
                .build();

        return Response.builder()
                .roommateMatchingRequiredInfo(roommateMatchingRequiredInfo)
                .build();
    }

    private void sendRequestMessage(Long chatRoomId, RoommateRequestDto.Response response) {
        RoommateRequestMessageEvent<RoommateRequestDto.Response> event = new RoommateRequestMessageEvent<>(chatRoomId, response);
        eventPublisher.publishEvent(event);
        event.requireCompleted();
    }

    @Transactional
    public Response acceptRequired(Long memberId, Long requestId) {
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
        sendRequestMessage(roommateMatchingRequired.getChattingRoomId(), response);

        return response;
    }

    private void changePrivacyStateToPrivate(Member requester, Member requestee) {
        MemberPrivacy requesterPrivacy = memberPrivacyService.findByMemberId(requester.getId()).getFirst();
        MemberPrivacy requesteePrivacy = memberPrivacyService.findByMemberId(requestee.getId()).getFirst();

        requesterPrivacy.changeState(MemberPrivacyType.PRIVATE);
        requesteePrivacy.changeState(MemberPrivacyType.PRIVATE);
    }

    @Transactional
    public Response rejectRequired(Long memberId, Long requestId) {
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
        sendRequestMessage(roommateMatchingRequired.getChattingRoomId(), response);
        return response;
    }

    @Transactional
    public Response cancelRequired(Long memberId, Long requestId) {
        RoommateMatchingRequired roommateMatchingRequired = roommateMatchingRequiredService.findByIdOrThrow(requestId);

        if (!roommateMatchingRequired.isRequester(memberId)) {
            throw new BusinessException(RequiredErrorCode.ROOMMATE_ACCESS_DENIED);
        }

        validateRequired(roommateMatchingRequired);
        roommateMatchingRequired.cancel();
        Response response = toDto(roommateMatchingRequired);
        sendRequestMessage(roommateMatchingRequired.getChattingRoomId(), response);
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
                .chatRoomId(roommateMatchingRequired.getChattingRoomId())
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
