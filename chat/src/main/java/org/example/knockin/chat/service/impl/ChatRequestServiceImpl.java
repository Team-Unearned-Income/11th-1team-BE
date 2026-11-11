package org.example.knockin.chat.service.impl;

import org.example.knockin.alarm.service.impl.ChattingRequiredAlarmServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.example.knockin.board.repository.RoommateBoardRepository;
import org.example.knockin.global.exception.RoommateBoardErrorCode;
import org.example.knockin.chat.dto.ChatRequestDetailDto;
import org.example.knockin.chat.dto.ChatRequestDetailDto.Response.Lifestyle;
import org.example.knockin.chat.dto.ChatRequestDetailDto.Response.MemberInfo;
import org.example.knockin.chat.dto.ChatRequestDto;
import org.example.knockin.chat.dto.ChatRequestListDto;
import org.example.knockin.board.entity.RoommateBoard;
import org.example.knockin.chat.entity.ChattingRequired;
import org.example.knockin.chat.entity.ChattingRequiredStatus;
import org.example.knockin.member.entity.Member;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.CommonErrorCode;
import org.example.knockin.global.exception.MemberErrorCode;
import org.example.knockin.global.exception.RequiredErrorCode;
import org.example.knockin.global.util.DateUtils;
import org.example.knockin.global.util.HasMemberId;
import org.example.knockin.chat.repository.row.ChatRequestListRow;
import org.example.knockin.life.repository.row.MatchingLifestyleRow;
import org.example.knockin.member.repository.row.ChattingRoomBasicInfoRow;
import org.example.knockin.util.service.RoommateScoreService;
import org.example.knockin.member.service.impl.BasicInformationServiceImpl;
import org.example.knockin.life.service.impl.MemberLifePatternService;
import org.example.knockin.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatRequestServiceImpl {
    private static final Map<ChattingRequiredStatus, String> ALARM_TEMPLATE_BY_STATUS = Map.of(
            ChattingRequiredStatus.PENDING, "%s님이 매칭을 요청했어요",
            ChattingRequiredStatus.ACCEPTED, "%s님이 매칭 요청을 수락했어요",
            ChattingRequiredStatus.REJECTED, "%s님이 매칭 요청을 거절했어요",
            ChattingRequiredStatus.CANCELED, "%s님이 매칭 요청을 취소했어요"
    );

    private final MemberRepository memberRepository;
    private final ChattingRequiredServiceImpl chattingRequiredService;
    private final RoommateBoardRepository roommateBoardRepository;
    private final ChattingRequiredAlarmServiceImpl chattingRequiredAlarmService;
    private final BasicInformationServiceImpl basicInformationService;
    private final MemberLifePatternService memberLifePatternService;
    private final RoommateScoreService javaRoommateScoreV2Service;

    @Transactional(readOnly = true)
    public List<ChatRequestListDto.Response> getPendingChatRequestList(Long memberId) {
        Member member = findMemberByIdOrThrow(memberId);

        List<ChatRequestListRow> requestListRows = chattingRequiredService.findAllPendingByRequesteeId(member.getId());
        List<Long> requesterIds = requestListRows.stream()
                .map(ChatRequestListRow::memberId)
                .distinct()
                .toList();
        Map<Long, Integer> scoresByMemberId = requesterIds.isEmpty()
                ? Map.of()
                : javaRoommateScoreV2Service.calculateSimpleScores(memberId, requesterIds);

        return requestListRows.stream()
                .map(row -> toResponse(row, scoresByMemberId.get(row.memberId())))
                .toList();
    }

    private ChatRequestListDto.Response toResponse(ChatRequestListRow row, Integer score) {
        return ChatRequestListDto.Response.builder()
                .requiredId(row.requiredId())
                .status(row.status())
                .memberId(row.memberId())
                .memberName(row.memberName())
                .memberAge(DateUtils.calculateAge(row.birth()))
                .gender(row.gender())
                .score(score)
                .createdAt(row.createdAt())
                .build();
    }

    @Transactional(readOnly = true)
    public ChatRequestDetailDto.Response getChatRequestDetail(Long memberId, Long requestId) {
        ChattingRequired chattingRequired = chattingRequiredService.findByIdOrThrow(requestId);

        boolean isRequester = isRequester(memberId, chattingRequired);
        Long myId = isRequester ? chattingRequired.getRequesterId() : chattingRequired.getRequesteeId();
        Long opponentId = isRequester ? chattingRequired.getRequesteeId() : chattingRequired.getRequesterId();

        List<ChattingRoomBasicInfoRow> basicInfoRows = basicInformationService.findChattingRoomBasicInfoRows(List.of(myId, opponentId));
        Map<Long, ChattingRoomBasicInfoRow> basicInfoRowMap = HasMemberId.toMapByMemberId(basicInfoRows);

        List<MatchingLifestyleRow> lifestyleRows = memberLifePatternService.findMatchingRowByMemberIdsIn(List.of(myId, opponentId));
        Map<Long, List<MatchingLifestyleRow>> lifeStyleRowMap = HasMemberId.groupingByMemberId(lifestyleRows);

        MemberInfo meInfo = toMemberInfo(myId, basicInfoRowMap.get(myId), lifeStyleRowMap.get(myId));
        MemberInfo opponentInfo = toMemberInfo(opponentId, basicInfoRowMap.get(opponentId), lifeStyleRowMap.get(opponentId));

        Integer score = javaRoommateScoreV2Service.calculateSimpleScore(myId, opponentId);

        return ChatRequestDetailDto.Response.builder()
                .requiredId(chattingRequired.getId())
                .status(chattingRequired.getStatus())
                .createdAt(chattingRequired.getCreatedAt())
                .score(score)
                .me(meInfo)
                .opponent(opponentInfo)
                .isRequester(isRequester)
                .build();
    }

    private ChatRequestDetailDto.Response.MemberInfo toMemberInfo(
            Long memberId,
            ChattingRoomBasicInfoRow basicInformationRow,
            List<MatchingLifestyleRow> lifestyleRows
    ) {
        if (basicInformationRow == null) {
            throw new BusinessException(MemberErrorCode.BASIC_INFO_NOT_FOUND);
        }

        List<Lifestyle> lifestyles = (lifestyleRows == null ? List.<MatchingLifestyleRow>of() : lifestyleRows)
                .stream()
                .map(this::toLifeStyle)
                .toList();

        return ChatRequestDetailDto.Response.MemberInfo.builder()
                .memberId(memberId)
                .memberName(basicInformationRow.name())
                .memberAge(DateUtils.calculateAge(basicInformationRow.birth()))
                .gender(basicInformationRow.gender())
                .memberProfileImageUrl(basicInformationRow.profileImageUrl())
                .lifeStyles(lifestyles)
                .build();
    }

    private ChatRequestDetailDto.Response.Lifestyle toLifeStyle(MatchingLifestyleRow row) {
        return ChatRequestDetailDto.Response.Lifestyle.builder()
                .lifestyleId(row.lifestyleId())
                .name(row.name())
                .value(row.value())
                .description(row.description())
                .type(row.type())
                .build();
    }

    private boolean isRequester(Long memberId, ChattingRequired chattingRequired) {
        Long requesterId = chattingRequired.getRequesterId();
        Long requesteeId = chattingRequired.getRequesteeId();

        if (requesterId.equals(memberId)) {
            return true;
        }

        if (requesteeId.equals(memberId)) {
            return false;
        }

        throw new BusinessException(RequiredErrorCode.CHATTING_ACCESS_DENIED);
    }

    @Transactional
    public ChatRequestDto.Response saveChatRequest(Long requesterId, ChatRequestDto.Request request) {
        validateChatRequest(requesterId, request);

        Member requester = findMemberByIdOrThrow(requesterId);

        Member requestee = findMemberByIdOrThrow(request.getRequesteeId());

        validateDuplicatePendingRequest(requester, requestee);

        RoommateBoard roommateBoard = request.getBoardId() == null
                ? null
                : roommateBoardRepository.findById(request.getBoardId())
                        .orElseThrow(() -> new BusinessException(RoommateBoardErrorCode.ROOMMATE_BOARD_NOT_FOUND));
        ChattingRequired chattingRequired = chattingRequiredService.savePending(requester.getId(), requestee.getId(), roommateBoard == null ? null : roommateBoard.getId());
        sendAlarm(requestee, requester, chattingRequired);

        return new ChatRequestDto.Response(LocalDateTime.now());
    }

    @Transactional
    public ChatRequestDto.Response acceptRequired(Long memberId, Long requestId) {
        ChattingRequired required = chattingRequiredService.findByIdOrThrow(requestId);

        if (!memberId.equals(required.getRequesteeId())) throw new BusinessException(RequiredErrorCode.CHATTING_ACCESS_DENIED);
        if (!required.isPending()) throw new BusinessException(RequiredErrorCode.CHATTING_INVALID_STATUS);

        required.accept();
        sendAlarm(findMemberByIdOrThrow(required.getRequesterId()), findMemberByIdOrThrow(required.getRequesteeId()), required);
        return new ChatRequestDto.Response(LocalDateTime.now());
    }

    @Transactional
    public ChatRequestDto.Response rejectRequired(Long memberId, Long requestId) {
        ChattingRequired required = chattingRequiredService.findByIdOrThrow(requestId);

        if (!memberId.equals(required.getRequesteeId())) throw new BusinessException(RequiredErrorCode.CHATTING_ACCESS_DENIED);
        if (!required.isPending()) throw new BusinessException(RequiredErrorCode.CHATTING_INVALID_STATUS);

        required.reject();
        sendAlarm(findMemberByIdOrThrow(required.getRequesterId()), findMemberByIdOrThrow(required.getRequesteeId()), required);
        return new ChatRequestDto.Response(LocalDateTime.now());
    }

    @Transactional
    public ChatRequestDto.Response cancelRequired(Long memberId, Long requestId) {
        ChattingRequired required = chattingRequiredService.findByIdOrThrow(requestId);

        if (!memberId.equals(required.getRequesterId())) throw new BusinessException(RequiredErrorCode.CHATTING_ACCESS_DENIED);
        if (!required.isPending()) throw new BusinessException(RequiredErrorCode.CHATTING_INVALID_STATUS);

        required.cancel();
        sendAlarm(findMemberByIdOrThrow(required.getRequesteeId()), findMemberByIdOrThrow(required.getRequesterId()), required);
        return new ChatRequestDto.Response(LocalDateTime.now());
    }

    private void validateChatRequest(Long requesterId, ChatRequestDto.Request request) {
        if (requesterId == null || request == null || request.getRequesteeId() == null || requesterId.equals(request.getRequesteeId())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }
    }

    private void validateDuplicatePendingRequest(Member requester, Member requestee) {
        chattingRequiredService.findLatest(requester.getId(), requestee.getId())
                .filter(ChattingRequired::isPending)
                .ifPresent(required -> {
                    throw new BusinessException(RequiredErrorCode.CHATTING_DUPLICATE);
                });
    }

    private void sendAlarm(Member requestee, Member requester, ChattingRequired chattingRequired) {
        chattingRequiredAlarmService.send(requestee, requester, chattingRequired.getId(), ALARM_TEMPLATE_BY_STATUS.get(chattingRequired.getStatus()));
    }
    private Member findMemberByIdOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
