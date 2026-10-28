package org.example.knockin.repository.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.example.knockin.chat.repository.ChattingRoomRepository;
import org.example.knockin.global.config.QueryDslConfig;
import org.example.knockin.chat.dto.MessageType;
import org.example.knockin.verification.entity.LoginProviderType;
import org.example.knockin.chat.entity.ChatRoomMember;
import org.example.knockin.chat.entity.ChatRoomMessage;
import org.example.knockin.chat.entity.ChattingRequired;
import org.example.knockin.chat.entity.ChattingRequiredStatus;
import org.example.knockin.chat.entity.ChattingRoom;
import org.example.knockin.member.entity.BasicInformationFile;
import org.example.knockin.meta.entity.File;
import org.example.knockin.meta.entity.FileType;
import org.example.knockin.member.entity.BasicInformation;
import org.example.knockin.member.entity.Gender;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberRole;
import org.example.knockin.mate.entity.MyRoommate;
import org.example.knockin.mate.entity.RoommateMatchingRequired;
import org.example.knockin.mate.entity.RoommateRequiredStatus;
import org.example.knockin.chat.repository.row.ChatRoomListRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
@DisplayName("채팅방 Repository")
class ChattingRoomRepositoryTest {

    private static final LocalDateTime CHAT_ROOM_CREATED_AT = LocalDateTime.of(2026, 6, 18, 12, 0);

    @Autowired
    private ChattingRoomRepository chattingRoomRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("회원이 참여 중인 채팅방 목록은 상대 회원 정보와 채팅방 상태를 반환하고 나간 방은 제외한다")
    void findByMemberIdReturnsActiveRoomsWithOtherMemberInfo() {
        // Given
        Member viewer = persistMember("viewer");
        Member activeOpponent = persistMember("active-opponent");
        Member pendingOpponent = persistMember("pending-opponent");
        Member leftOpponent = persistMember("left-opponent");
        Member unrelatedMember = persistMember("unrelated");
        persistBasicInformationWithProfile(activeOpponent, "상대회원", "opponent-profile.jpg");
        persistBasicInformationWithProfile(pendingOpponent, "대기상대", "pending-profile.jpg");
        persistBasicInformationWithProfile(leftOpponent, "나간방상대", "left-profile.jpg");
        persistBasicInformationWithProfile(unrelatedMember, "무관회원", "unrelated-profile.jpg");

        ChattingRoom activeRoom = persistChattingRoom(viewer, activeOpponent, ChattingRequiredStatus.ACCEPTED);
        persistChatRoomMember(activeRoom, viewer, false);
        persistChatRoomMember(activeRoom, activeOpponent, false);

        ChattingRoom pendingRoom = persistChattingRoom(viewer, pendingOpponent, ChattingRequiredStatus.PENDING);
        persistChatRoomMember(pendingRoom, viewer, false);
        persistChatRoomMember(pendingRoom, pendingOpponent, false);

        ChattingRoom leftRoom = persistChattingRoom(viewer, leftOpponent, ChattingRequiredStatus.ACCEPTED);
        persistChatRoomMember(leftRoom, viewer, true);
        persistChatRoomMember(leftRoom, leftOpponent, false);

        ChattingRoom unrelatedRoom = persistChattingRoom(activeOpponent, unrelatedMember, ChattingRequiredStatus.REJECTED);
        persistChatRoomMember(unrelatedRoom, activeOpponent, false);
        persistChatRoomMember(unrelatedRoom, unrelatedMember, false);

        entityManager.flush();
        entityManager.clear();

        // When
        List<ChatRoomListRow> responses = chattingRoomRepository.findListRowsByMemberId(viewer.getId());

        // Then
        assertThat(responses).hasSize(2);
        ChatRoomListRow acceptedResponse = findResponseByChatRoomId(responses, activeRoom.getId());
        assertThat(acceptedResponse.memberName()).isEqualTo("상대회원");
        assertThat(acceptedResponse.memberProfileImageUrl()).isEqualTo("opponent-profile.jpg");
        assertThat(acceptedResponse.createdAt()).isEqualTo(CHAT_ROOM_CREATED_AT);
        assertThat(acceptedResponse.roommateStatus()).isNull();
        assertThat(acceptedResponse.isRoommate()).isFalse();
        assertThat(acceptedResponse.lastMessage()).isNull();

        ChatRoomListRow pendingResponse = findResponseByChatRoomId(responses, pendingRoom.getId());
        assertThat(pendingResponse.memberName()).isEqualTo("대기상대");
        assertThat(pendingResponse.memberProfileImageUrl()).isEqualTo("pending-profile.jpg");
        assertThat(pendingResponse.createdAt()).isEqualTo(CHAT_ROOM_CREATED_AT);
        assertThat(pendingResponse.roommateStatus()).isNull();
        assertThat(pendingResponse.isRoommate()).isFalse();
        assertThat(pendingResponse.lastMessage()).isNull();
    }

    @Test
    @DisplayName("상대 회원의 프로필 이미지가 없어도 채팅방 목록에 포함한다")
    void findByMemberIdReturnsRoomWhenOpponentHasNoProfileImage() {
        // Given
        Member viewer = persistMember("viewer-without-opponent-profile-image");
        Member opponent = persistMember("opponent-without-profile-image");
        BasicInformation basicInformation = BasicInformation.builder()
                .member(opponent)
                .name("프로필없는상대")
                .birth(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .email("opponent-without-profile@example.com")
                .build();
        entityManager.persist(basicInformation);

        ChattingRoom room = persistChattingRoom(viewer, opponent, ChattingRequiredStatus.ACCEPTED);
        persistChatRoomMember(room, viewer, false);
        persistChatRoomMember(room, opponent, false);
        entityManager.flush();
        entityManager.clear();

        // When
        List<ChatRoomListRow> responses = chattingRoomRepository.findListRowsByMemberId(viewer.getId());

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().chatRoomId()).isEqualTo(room.getId());
        assertThat(responses.getFirst().memberName()).isEqualTo("프로필없는상대");
        assertThat(responses.getFirst().memberProfileImageUrl()).isNull();
    }

    @Test
    @DisplayName("채팅방 목록은 해당 방의 마지막 메시지를 함께 반환한다")
    void findByMemberIdReturnsLastMessage() {
        // Given
        Member viewer = persistMember("viewer-last-message");
        Member opponent = persistMember("opponent-last-message");
        persistBasicInformationWithProfile(opponent, "마지막메시지상대", "last-message-profile.jpg");
        ChattingRoom room = persistChattingRoom(viewer, opponent, ChattingRequiredStatus.ACCEPTED);
        ChatRoomMember viewerRoomMember = persistChatRoomMember(room, viewer, false);
        ChatRoomMember opponentRoomMember = persistChatRoomMember(room, opponent, false);
        ChatRoomMessage previousMessage =
                persistChatRoomMessage(room, viewerRoomMember.getMember(), "이전 메시지");
        ChatRoomMessage latestMessage =
                persistChatRoomMessage(room, opponentRoomMember.getMember(), "최근 메시지");

        entityManager.flush();
        updateMessageCreatedAt(previousMessage, LocalDateTime.of(2026, 6, 18, 12, 10));
        updateMessageCreatedAt(latestMessage, LocalDateTime.of(2026, 6, 18, 12, 20));
        entityManager.clear();

        // When
        List<ChatRoomListRow> responses = chattingRoomRepository.findListRowsByMemberId(viewer.getId());

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().lastMessage()).isEqualTo("최근 메시지");
        assertThat(responses.getFirst().lastMessageAt())
                .isEqualTo(LocalDateTime.of(2026, 6, 18, 12, 20));
    }

    @Test
    @DisplayName("채팅방 목록은 최신 룸메이트 요청 상태와 현재 룸메이트 여부를 반환한다")
    void findListRowsReturnsLatestRoommateStatusAndActiveRoommateFlag() {
        // Given
        Member viewer = persistMember("viewer-roommate-status");
        Member opponent = persistMember("opponent-roommate-status");
        persistBasicInformationWithProfile(opponent, "룸메이트상대", "roommate-profile.jpg");
        ChattingRoom room = persistChattingRoom(viewer, opponent, ChattingRequiredStatus.ACCEPTED);
        persistChatRoomMember(room, viewer, false);
        persistChatRoomMember(room, opponent, false);

        RoommateMatchingRequired accepted = persistRoommateRequest(
                room,
                viewer,
                opponent,
                RoommateRequiredStatus.ACCEPTED
        );
        entityManager.persist(MyRoommate.builder()
                .roommateMatchingRequired(accepted)
                .isDeleted(false)
                .build());
        persistRoommateRequest(room, opponent, viewer, RoommateRequiredStatus.PENDING);

        entityManager.flush();
        entityManager.clear();

        // When
        ChatRoomListRow response =
                chattingRoomRepository.findListRowsByMemberId(viewer.getId()).getFirst();

        // Then
        assertThat(response.roommateStatus()).isEqualTo(RoommateRequiredStatus.PENDING);
        assertThat(response.isRoommate()).isTrue();
    }

    @Test
    @DisplayName("채팅방 목록은 마지막 메시지 시각이 최근인 순서로 정렬한다")
    void findListRowsOrdersByLastMessageAtDescending() {
        // Given
        Member viewer = persistMember("viewer-last-message-order");
        Member recentOpponent = persistMember("recent-opponent");
        Member oldOpponent = persistMember("old-opponent");
        persistBasicInformationWithProfile(recentOpponent, "최근상대", "recent.jpg");
        persistBasicInformationWithProfile(oldOpponent, "오래된상대", "old.jpg");

        ChattingRoom recentRoom = persistChattingRoom(viewer, recentOpponent, ChattingRequiredStatus.ACCEPTED);
        persistChatRoomMember(recentRoom, viewer, false);
        persistChatRoomMember(recentRoom, recentOpponent, false);
        ChatRoomMessage recentMessage = persistChatRoomMessage(recentRoom, recentOpponent, "최근 메시지");

        ChattingRoom oldRoom = persistChattingRoom(viewer, oldOpponent, ChattingRequiredStatus.ACCEPTED);
        persistChatRoomMember(oldRoom, viewer, false);
        persistChatRoomMember(oldRoom, oldOpponent, false);
        ChatRoomMessage oldMessage = persistChatRoomMessage(oldRoom, oldOpponent, "오래된 메시지");

        entityManager.flush();
        updateMessageCreatedAt(recentMessage, LocalDateTime.of(2026, 6, 18, 15, 0));
        updateMessageCreatedAt(oldMessage, LocalDateTime.of(2026, 6, 18, 13, 0));
        entityManager.clear();

        // When
        List<ChatRoomListRow> responses = chattingRoomRepository.findListRowsByMemberId(viewer.getId());

        // Then
        assertThat(responses).extracting(ChatRoomListRow::chatRoomId)
                .containsExactly(recentRoom.getId(), oldRoom.getId());
    }

    @Test
    @DisplayName("같은 채팅방에는 같은 회원이 한 번만 참여할 수 있다")
    void chatRoomMemberRejectsDuplicateRoomMember() {
        // Given
        Member viewer = persistMember("viewer-duplicate-room-member");
        Member opponent = persistMember("opponent-duplicate-room-member");
        ChattingRoom room = persistChattingRoom(viewer, opponent, ChattingRequiredStatus.ACCEPTED);
        persistChatRoomMember(room, viewer, false);

        // When & Then
        assertThatThrownBy(() -> {
            persistChatRoomMember(room, viewer, false);
            entityManager.flush();
            entityManager.clear();
        }).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("참여 중인 채팅방이 없으면 빈 목록을 반환한다")
    void findByMemberIdReturnsEmptyListWhenMemberHasNoActiveRooms() {
        // Given
        Member viewer = persistMember("viewer-empty");
        Member opponent = persistMember("opponent-empty");
        ChattingRoom room = persistChattingRoom(viewer, opponent, ChattingRequiredStatus.ACCEPTED);
        persistChatRoomMember(room, viewer, true);
        persistChatRoomMember(room, opponent, false);
        entityManager.flush();
        entityManager.clear();

        // When
        List<ChatRoomListRow> responses = chattingRoomRepository.findListRowsByMemberId(viewer.getId());

        // Then
        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("두 회원이 모두 참여 중인 채팅방이 있으면 활성 채팅방이 있다고 조회한다")
    void existsActiveRoomBetweenMembersReturnsTrueWhenBothMembersAreActive() {
        // Given
        Member memberA = persistMember("active-room-member-a");
        Member memberB = persistMember("active-room-member-b");
        Member otherMember = persistMember("active-room-other-member");

        ChattingRoom activeRoom = persistChattingRoom(memberA, memberB, ChattingRequiredStatus.ACCEPTED);
        persistChatRoomMember(activeRoom, memberA, false);
        persistChatRoomMember(activeRoom, memberB, false);

        ChattingRoom leftRoom = persistChattingRoom(memberA, otherMember, ChattingRequiredStatus.ACCEPTED);
        persistChatRoomMember(leftRoom, memberA, false);
        persistChatRoomMember(leftRoom, otherMember, true);
        entityManager.flush();
        entityManager.clear();

        // When
        boolean exists = chattingRoomRepository.existsActiveRoomBetweenMembers(memberA.getId(), memberB.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("한 회원이 나간 채팅방만 있으면 활성 채팅방이 없다고 조회한다")
    void existsActiveRoomBetweenMembersReturnsFalseWhenMemberLeft() {
        // Given
        Member memberA = persistMember("left-room-member-a");
        Member memberB = persistMember("left-room-member-b");
        ChattingRoom room = persistChattingRoom(memberA, memberB, ChattingRequiredStatus.ACCEPTED);
        persistChatRoomMember(room, memberA, false);
        persistChatRoomMember(room, memberB, true);
        entityManager.flush();
        entityManager.clear();

        // When
        boolean exists = chattingRoomRepository.existsActiveRoomBetweenMembers(memberA.getId(), memberB.getId());

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("회원의 활성 채팅방 수를 조회할 때 나간 방은 제외한다")
    void countActiveRoomsByMemberIdExcludesLeftRooms() {
        // Given
        Member viewer = persistMember("room-count-viewer");
        Member activeOpponent = persistMember("room-count-active-opponent");
        Member leftOpponent = persistMember("room-count-left-opponent");
        Member unrelatedA = persistMember("room-count-unrelated-a");
        Member unrelatedB = persistMember("room-count-unrelated-b");

        ChattingRoom activeRoom = persistChattingRoom(viewer, activeOpponent, ChattingRequiredStatus.ACCEPTED);
        persistChatRoomMember(activeRoom, viewer, false);
        persistChatRoomMember(activeRoom, activeOpponent, false);

        ChattingRoom leftRoom = persistChattingRoom(viewer, leftOpponent, ChattingRequiredStatus.ACCEPTED);
        persistChatRoomMember(leftRoom, viewer, true);
        persistChatRoomMember(leftRoom, leftOpponent, false);

        ChattingRoom unrelatedRoom = persistChattingRoom(unrelatedA, unrelatedB, ChattingRequiredStatus.ACCEPTED);
        persistChatRoomMember(unrelatedRoom, unrelatedA, false);
        persistChatRoomMember(unrelatedRoom, unrelatedB, false);
        entityManager.flush();
        entityManager.clear();

        // When
        long count = chattingRoomRepository.countActiveRoomsByMemberId(viewer.getId());

        // Then
        assertThat(count).isEqualTo(1L);
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

    private BasicInformation persistBasicInformationWithProfile(Member member, String name, String savedFileName) {
        BasicInformation basicInformation = BasicInformation.builder()
                .member(member)
                .name(name)
                .birth(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .email(name + "@example.com")
                .build();
        entityManager.persist(basicInformation);

        File file = File.builder()
                .type(FileType.USER_PROFILE_IMAGE)
                .originalFileName(savedFileName)
                .savedFileName(savedFileName)
                .fileExt("jpg")
                .isDeleted(false)
                .build();
        entityManager.persist(file);

        BasicInformationFile basicInformationFile = newBasicInformationFile(basicInformation, file);
        entityManager.persist(basicInformationFile);
        return basicInformation;
    }

    private BasicInformationFile newBasicInformationFile(BasicInformation basicInformation, File file) {
        try {
            Constructor<BasicInformationFile> constructor = BasicInformationFile.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            BasicInformationFile basicInformationFile = constructor.newInstance();
            ReflectionTestUtils.setField(basicInformationFile, "basicInformation", basicInformation);
            ReflectionTestUtils.setField(basicInformationFile, "fileId", file.getId());
            return basicInformationFile;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("기본 정보 파일 테스트 엔티티 생성에 실패했습니다.", e);
        }
    }

    private ChatRoomListRow findResponseByChatRoomId(List<ChatRoomListRow> responses, Long chatRoomId) {
        return responses.stream()
                .filter(response -> response.chatRoomId().equals(chatRoomId))
                .findFirst()
                .orElseThrow();
    }

    private ChattingRoom persistChattingRoom(Member requester, Member requestee, ChattingRequiredStatus status) {
        ChattingRequired chattingRequired = ChattingRequired.builder()
                .requester(requester)
                .requestee(requestee)
                .status(status)
                .build();
        entityManager.persist(chattingRequired);

        ChattingRoom chattingRoom = ChattingRoom.builder()
                .chattingRequired(chattingRequired)
                .build();
        entityManager.persist(chattingRoom);
        entityManager.flush();
        entityManager.createNativeQuery("update chatting_room set created_at = ? where id = ?")
                .setParameter(1, Timestamp.valueOf(CHAT_ROOM_CREATED_AT))
                .setParameter(2, chattingRoom.getId())
                .executeUpdate();
        return chattingRoom;
    }

    private ChatRoomMember persistChatRoomMember(ChattingRoom chattingRoom, Member member, Boolean isLeft) {
        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                .chattingRoom(chattingRoom)
                .member(member)
                .isLeft(isLeft)
                .build();
        entityManager.persist(chatRoomMember);
        return chatRoomMember;
    }

    private ChatRoomMessage persistChatRoomMessage(ChattingRoom chattingRoom, Member member, String contents) {
        return persistChatRoomMessage(chattingRoom, member, contents, false);
    }

    private ChatRoomMessage persistChatRoomMessage(
            ChattingRoom chattingRoom,
            Member member,
            String contents,
            Boolean isRead
    ) {
        ChatRoomMessage chatRoomMessage = ChatRoomMessage.builder()
                .chattingRoom(chattingRoom)
                .member(member)
                .type(MessageType.TEXT)
                .contents(contents)
                .isRead(isRead)
                .build();
        entityManager.persist(chatRoomMessage);
        return chatRoomMessage;
    }

    private RoommateMatchingRequired persistRoommateRequest(
            ChattingRoom chattingRoom,
            Member requester,
            Member requestee,
            RoommateRequiredStatus status
    ) {
        RoommateMatchingRequired required = RoommateMatchingRequired.builder()
                .chattingRoom(chattingRoom)
                .requester(requester)
                .requestee(requestee)
                .status(status)
                .build();
        entityManager.persist(required);
        return required;
    }

    private void updateMessageCreatedAt(ChatRoomMessage message, LocalDateTime createdAt) {
        entityManager.createNativeQuery("update chat_room_message set created_at = ? where id = ?")
                .setParameter(1, Timestamp.valueOf(createdAt))
                .setParameter(2, message.getId())
                .executeUpdate();
    }
}
