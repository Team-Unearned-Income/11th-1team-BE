package org.example.knockin.chat.repository.impl;

import static org.example.knockin.chat.entity.QChatRoomFile.chatRoomFile;
import static org.example.knockin.chat.entity.QChatRoomMessage.chatRoomMessage;
import static org.example.knockin.meta.entity.QFile.file;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.knockin.chat.dto.ChatRoomDetailDto.ChatMessage;
import org.example.knockin.chat.entity.QChatRoomMember;
import org.example.knockin.chat.entity.QChatRoomMessage;
import org.example.knockin.chat.repository.ChatRoomMessageRepositoryCustom;
import org.example.knockin.chat.repository.row.ChatRoomUnreadCountRow;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChatRoomMessageRepositoryImpl implements ChatRoomMessageRepositoryCustom {
    public final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<ChatMessage> findChatMessageDto(Long chatRoomId) {
        return jpaQueryFactory
                .select(Projections.constructor(
                        ChatMessage.class,
                        chatRoomMessage.id,
                        chatRoomMessage.memberId,
                        chatRoomMessage.contents,
                        chatRoomMessage.createdAt,
                        chatRoomMessage.type,
                        file.savedFileName
                ))
                .from(chatRoomMessage)
                .leftJoin(chatRoomFile)
                .on(chatRoomFile.chatRoomMessage.eq(chatRoomMessage))
                .leftJoin(file).on(chatRoomFile.fileId.eq(file.id))
                .where(chatRoomMessage.chattingRoom.id.eq(chatRoomId))
                .orderBy(chatRoomMessage.createdAt.asc(), chatRoomMessage.id.asc())
                .fetch();
    }

    @Override
    public List<ChatRoomUnreadCountRow> findUnreadMessageCounts(Long memberId, List<Long> chatRoomIds) {
        if (chatRoomIds.isEmpty()) return List.of();

        QChatRoomMember viewerRoomMember = new QChatRoomMember("unreadViewerRoomMember");
        QChatRoomMessage unreadMessage = new QChatRoomMessage("unreadMessage");

        return jpaQueryFactory
                .select(Projections.constructor(
                        ChatRoomUnreadCountRow.class,
                        unreadMessage.chattingRoom.id,
                        unreadMessage.count()
                ))
                .from(unreadMessage)
                .join(viewerRoomMember)
                .on(
                        viewerRoomMember.chattingRoom.eq(unreadMessage.chattingRoom),
                        viewerRoomMember.memberId.eq(memberId),
                        viewerRoomMember.isLeft.isFalse()
                )
                .where(
                        unreadMessage.chattingRoom.id.in(chatRoomIds),
                        unreadMessage.isRead.isFalse(),
                        unreadMessage.memberId.isNull().or(unreadMessage.memberId.ne(memberId))
                )
                .groupBy(unreadMessage.chattingRoom.id)
                .fetch();
    }

    @Override
    public long markUnreadMessagesAsRead(Long chatRoomId, Long memberId) {
        return jpaQueryFactory
                .update(chatRoomMessage)
                .set(chatRoomMessage.isRead, true)
                .where(
                        chatRoomMessage.chattingRoom.id.eq(chatRoomId),
                        chatRoomMessage.isRead.isFalse(),
                        chatRoomMessage.memberId.isNull().or(chatRoomMessage.memberId.ne(memberId))
                )
                .execute();
    }
}
