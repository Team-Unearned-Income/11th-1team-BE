package org.example.knockin.chat.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.knockin.chat.entity.ChatRoomMember;
import org.example.knockin.chat.entity.ChattingRoom;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.chat.exception.ChattingErrorCode;
import org.example.knockin.chat.repository.ChatRoomMemberRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRoomMemberServiceImpl {

    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public ChatRoomMember findActiveMemberByRoomIdAndMemberId(Long chatRoomId, Long memberId) {
        return chatRoomMemberRepository.findActiveMemberByRoomIdAndMemberId(chatRoomId, memberId)
                .orElseThrow(() -> new BusinessException(ChattingErrorCode.ROOM_MEMBER_NOT_FOUND));
    }

    public Long findPartnerMemberId(ChatRoomMember me, Long chatRoomId) {
        return chatRoomMemberRepository.findPartnerMemberId(me, chatRoomId);
    }

    public List<ChatRoomMember> saveAll(ChattingRoom chattingRoom, List<Long> members) {
        if (members.size() > 2) {
            throw new BusinessException(ChattingErrorCode.ROOM_CAPACITY_EXCEEDED);
        }

        List<ChatRoomMember> chatRoomMembers = members.stream()
                .map(member -> ChatRoomMember.of(chattingRoom, member))
                .toList();

        return chatRoomMemberRepository.saveAll(chatRoomMembers);
    }

    public void checkCanSubscribe(Long chatRoomId, Long memberId) {
        if (!chatRoomMemberRepository.existsActiveMember(chatRoomId, memberId)) {
            throw new BusinessException(ChattingErrorCode.ROOM_ACCESS_DENIED);
        }
    }

    public void checkCanSendMessage(Long chatRoomId, Long memberId) {
        if (!chatRoomMemberRepository.existsActiveMember(chatRoomId, memberId)) {
            throw new BusinessException(ChattingErrorCode.ROOM_MEMBER_NOT_FOUND);
        }
    }

    public List<ChatRoomMember> findChatRoomMemberById(Long chatRoomId) {
        return chatRoomMemberRepository.findChatRoomMemberById(chatRoomId);
    }
}
