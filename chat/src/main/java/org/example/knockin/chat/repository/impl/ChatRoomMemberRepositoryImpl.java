package org.example.knockin.chat.repository.impl;

import static org.example.knockin.chat.entity.QChattingRoom.chattingRoom;
import static org.example.knockin.chat.entity.QChatRoomMember.chatRoomMember;

import com.querydsl.jpa.impl.JPAQueryFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.knockin.chat.entity.ChatRoomMember;
import org.example.knockin.chat.entity.ChattingRoom;
import org.example.knockin.chat.repository.ChatRoomMemberRepositoryCustom;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Repository;

@NullMarked
@Repository
@RequiredArgsConstructor
public class ChatRoomMemberRepositoryImpl implements ChatRoomMemberRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public boolean existsActiveMember(Long chatRoomId, Long memberId) {
        Integer result = jpaQueryFactory
                .selectOne()
                .from(chatRoomMember)
                .where(
                        chatRoomMember.chattingRoom.id.eq(chatRoomId),
                        chatRoomMember.memberId.eq(memberId),
                        chatRoomMember.isLeft.isFalse()
                )
                .fetchFirst();

        return result != null;
    }

    @Override
    public Optional<ChatRoomMember> findActiveMemberByRoomIdAndMemberId(Long chatRoomId, Long memberId) {
        return Optional.ofNullable(jpaQueryFactory
                .select(chatRoomMember)
                .from(chatRoomMember)
                .where(
                        chatRoomMember.chattingRoom.id.eq(chatRoomId),
                        chatRoomMember.memberId.eq(memberId),
                        chatRoomMember.isLeft.isFalse())
                .fetchFirst()
        );
    }

    @Override
    public Long findPartnerMemberId(ChatRoomMember me, ChattingRoom chattingRoomEntity) {
        return Objects.requireNonNull(jpaQueryFactory
                .select(chatRoomMember.memberId)
                .from(chatRoomMember)
                .join(chatRoomMember.chattingRoom, chattingRoom)
                .where(
                        chatRoomMember.chattingRoom.eq(chattingRoomEntity),
                        chatRoomMember.ne(me)
                )
                .fetchFirst());
    }

    @Override
    public Long findPartnerMemberId(ChatRoomMember me, Long chattingRoomId) {
        return Objects.requireNonNull(jpaQueryFactory
                .select(chatRoomMember.memberId)
                .from(chatRoomMember)
                .join(chatRoomMember.chattingRoom, chattingRoom)
                .where(
                        chatRoomMember.chattingRoom.id.eq(chattingRoomId),
                        chatRoomMember.ne(me)
                )
                .fetchFirst());
    }

    @Override
    public List<ChatRoomMember> findChatRoomMemberById(Long chatRoomId) {
        return jpaQueryFactory
                .select(chatRoomMember)
                .from(chatRoomMember)
                .join(chatRoomMember.chattingRoom, chattingRoom)
                .where(chatRoomMember.chattingRoom.id.eq(chatRoomId))
                .fetch();
    }
}
