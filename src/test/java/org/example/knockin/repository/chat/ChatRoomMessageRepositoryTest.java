package org.example.knockin.repository.chat;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.example.knockin.chat.repository.ChatRoomMessageRepository;
import org.example.knockin.global.config.QueryDslConfig;
import org.example.knockin.chat.dto.MessageType;
import org.example.knockin.verification.entity.LoginProviderType;
import org.example.knockin.chat.entity.ChatRoomMember;
import org.example.knockin.chat.entity.ChatRoomMessage;
import org.example.knockin.chat.entity.ChattingRequired;
import org.example.knockin.chat.entity.ChattingRequiredStatus;
import org.example.knockin.chat.entity.ChattingRoom;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberRole;
import org.example.knockin.chat.repository.row.ChatRoomUnreadCountRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
@DisplayName("채팅 메시지 Repository")
class ChatRoomMessageRepositoryTest {

    @Autowired
    private ChatRoomMessageRepository chatRoomMessageRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("안 읽은 메시지 수는 상대방 및 시스템 메시지만 집계한다")
    void findUnreadMessageCountsExcludesReadAndOwnMessages() {
        // Given
        Member viewer = persistMember("viewer-unread-count");
        Member opponent = persistMember("opponent-unread-count");
        ChattingRoom room = persistChattingRoom(viewer, opponent);
        persistChatRoomMember(room, viewer);
        persistChatRoomMember(room, opponent);

        persistChatRoomMessage(room, opponent, "이미 읽은 메시지", true);
        persistChatRoomMessage(room, opponent, "안 읽은 상대방 메시지", false);
        persistChatRoomMessage(room, viewer, "내가 보낸 메시지", false);
        persistChatRoomMessage(room, null, "안 읽은 시스템 메시지", false);

        entityManager.flush();
        entityManager.clear();

        // When
        List<ChatRoomUnreadCountRow> counts = chatRoomMessageRepository.findUnreadMessageCounts(
                viewer.getId(),
                List.of(room.getId())
        );

        // Then
        assertThat(counts).singleElement()
                .satisfies(count -> {
                    assertThat(count.chatRoomId()).isEqualTo(room.getId());
                    assertThat(count.messageCount()).isEqualTo(2L);
                });
    }

    @Test
    @DisplayName("채팅방 입장 시 상대방과 시스템의 안 읽은 메시지만 읽음 처리한다")
    void markUnreadMessagesAsReadUpdatesOnlyUnreadMessagesNotSentByViewer() {
        // Given
        Member viewer = persistMember("viewer-mark-read");
        Member opponent = persistMember("opponent-mark-read");
        ChattingRoom room = persistChattingRoom(viewer, opponent);
        ChattingRoom otherRoom = persistChattingRoom(viewer, opponent);
        persistChatRoomMember(room, viewer);
        persistChatRoomMember(room, opponent);
        persistChatRoomMember(otherRoom, viewer);
        persistChatRoomMember(otherRoom, opponent);

        ChatRoomMessage opponentUnread = persistChatRoomMessage(room, opponent, "안 읽은 상대방 메시지", false);
        ChatRoomMessage systemUnread = persistChatRoomMessage(room, null, "안 읽은 시스템 메시지", false);
        ChatRoomMessage ownUnread = persistChatRoomMessage(room, viewer, "내가 보낸 안 읽은 메시지", false);
        ChatRoomMessage opponentRead = persistChatRoomMessage(room, opponent, "이미 읽은 상대방 메시지", true);
        ChatRoomMessage otherRoomUnread =
                persistChatRoomMessage(otherRoom, opponent, "다른 채팅방의 안 읽은 메시지", false);

        entityManager.flush();
        entityManager.clear();

        // When
        long updatedCount = chatRoomMessageRepository.markUnreadMessagesAsRead(room.getId(), viewer.getId());
        entityManager.clear();

        // Then
        assertThat(updatedCount).isEqualTo(2L);
        assertThat(findMessage(opponentUnread.getId()).getIsRead()).isTrue();
        assertThat(findMessage(systemUnread.getId()).getIsRead()).isTrue();
        assertThat(findMessage(ownUnread.getId()).getIsRead()).isFalse();
        assertThat(findMessage(opponentRead.getId()).getIsRead()).isTrue();
        assertThat(findMessage(otherRoomUnread.getId()).getIsRead()).isFalse();
        assertThat(chatRoomMessageRepository.findUnreadMessageCounts(viewer.getId(), List.of(room.getId())))
                .isEmpty();

        assertThat(chatRoomMessageRepository.markUnreadMessagesAsRead(room.getId(), viewer.getId()))
                .isZero();
    }

    private ChatRoomMessage findMessage(Long messageId) {
        return chatRoomMessageRepository.findById(messageId).orElseThrow();
    }

    private Member persistMember(String providerId) {
        Member member = Member.builder()
                .providerType(LoginProviderType.KAKAO)
                .providerId(providerId)
                .role(MemberRole.USER)
                .isDelete(false)
                .build();
        entityManager.persist(member);
        return member;
    }

    private ChattingRoom persistChattingRoom(Member requester, Member requestee) {
        ChattingRequired chattingRequired = ChattingRequired.builder()
                .requesterId(requester.getId())
                .requesteeId(requestee.getId())
                .status(ChattingRequiredStatus.ACCEPTED)
                .build();
        entityManager.persist(chattingRequired);

        ChattingRoom chattingRoom = ChattingRoom.builder()
                .chattingRequired(chattingRequired)
                .build();
        entityManager.persist(chattingRoom);
        return chattingRoom;
    }

    private void persistChatRoomMember(ChattingRoom chattingRoom, Member member) {
        entityManager.persist(ChatRoomMember.builder()
                .chattingRoom(chattingRoom)
                .memberId(member == null ? null : member.getId())
                .isLeft(false)
                .build());
    }

    private ChatRoomMessage persistChatRoomMessage(
            ChattingRoom chattingRoom,
            Member member,
            String contents,
            Boolean isRead
    ) {
        ChatRoomMessage message = ChatRoomMessage.builder()
                .chattingRoom(chattingRoom)
                .memberId(member == null ? null : member.getId())
                .type(MessageType.TEXT)
                .contents(contents)
                .isRead(isRead)
                .build();
        entityManager.persist(message);
        return message;
    }
}
