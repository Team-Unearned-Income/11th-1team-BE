package org.example.knockin.chat.repository;

import java.util.List;
import java.util.Optional;
import org.example.knockin.chat.entity.ChatRoomMember;
import org.example.knockin.chat.entity.ChattingRoom;

public interface ChatRoomMemberRepositoryCustom {
    boolean existsActiveMember(Long chatRoomId, Long memberId);

    Optional<ChatRoomMember> findActiveMemberByRoomIdAndMemberId(Long chatRoomId, Long memberId);

    Long findPartnerMemberId(ChatRoomMember me, ChattingRoom chattingRoom);

    Long findPartnerMemberId(ChatRoomMember me, Long chattingRoomId);

    List<ChatRoomMember> findChatRoomMemberById(Long chatRoomId);
}
