package org.example.knockin.repository.room;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.example.knockin.global.config.QueryDslConfig;
import org.example.knockin.verification.entity.LoginProviderType;
import org.example.knockin.chat.entity.ChattingRequired;
import org.example.knockin.chat.entity.ChattingRequiredStatus;
import org.example.knockin.chat.entity.ChattingRoom;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberRole;
import org.example.knockin.mate.entity.MyRoommate;
import org.example.knockin.mate.entity.RoommateMatchingRequired;
import org.example.knockin.mate.entity.RoommateRequiredStatus;
import org.example.knockin.mate.repository.MyRoommateRepository;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
@DisplayName("내 룸메이트 리포지토리")
class MyRoommateRepositoryTest {

    @Autowired
    private MyRoommateRepository myRoommateRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("요청자나 피요청자 회원 ID로 내 룸메이트와 룸메이트 요청 정보를 조회한다")
    void findWithFetchedByMemberIdReturnsMyRoommateForRequesterOrRequestee() {
        // Given
        Member requester = persistMember("my-roommate-requester");
        Member requestee = persistMember("my-roommate-requestee");
        MyRoommate myRoommate = persistMyRoommate(requester, requestee);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<MyRoommate> requesterResult = myRoommateRepository.findWithRequiredByMemberId(requester.getId());
        Optional<MyRoommate> requesteeResult = myRoommateRepository.findWithRequiredByMemberId(requestee.getId());

        // Then
        assertThat(requesterResult).isPresent();
        assertThat(requesterResult.get().getId()).isEqualTo(myRoommate.getId());
        assertThat(Hibernate.isInitialized(requesterResult.get().getRoommateMatchingRequired())).isTrue();
        assertThat(requesterResult.get().getRoommateMatchingRequired().getRequester().getId()).isEqualTo(requester.getId());
        assertThat(requesterResult.get().getRoommateMatchingRequired().getRequestee().getId()).isEqualTo(requestee.getId());

        assertThat(requesteeResult).isPresent();
        assertThat(requesteeResult.get().getId()).isEqualTo(myRoommate.getId());
    }

    @Test
    @DisplayName("룸메이트 요청에 포함되지 않은 회원 ID이면 빈 값을 반환한다")
    void findWithFetchedByMemberIdReturnsEmptyWhenMemberIsNotRequesterOrRequestee() {
        // Given
        Member requester = persistMember("my-roommate-requester-empty");
        Member requestee = persistMember("my-roommate-requestee-empty");
        Member unrelated = persistMember("my-roommate-unrelated");
        persistMyRoommate(requester, requestee);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<MyRoommate> result = myRoommateRepository.findWithRequiredByMemberId(unrelated.getId());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("삭제된 내 룸메이트는 회원 ID로 조회되지 않고 존재하지 않는 것으로 판단한다")
    void findWithFetchedByMemberIdAndExistsExcludeDeletedMyRoommate() {
        // Given
        Member requester = persistMember("my-roommate-deleted-requester");
        Member requestee = persistMember("my-roommate-deleted-requestee");
        persistMyRoommate(requester, requestee, true);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<MyRoommate> result = myRoommateRepository.findWithRequiredByMemberId(requester.getId());
        boolean exists = myRoommateRepository.isExistRoomMate(requester);

        // Then
        assertThat(result).isEmpty();
        assertThat(exists).isFalse();
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

    private MyRoommate persistMyRoommate(Member requester, Member requestee) {
        return persistMyRoommate(requester, requestee, false);
    }

    private MyRoommate persistMyRoommate(Member requester, Member requestee, Boolean isDeleted) {
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

        RoommateMatchingRequired matchingRequired = RoommateMatchingRequired.builder()
                .requester(requester)
                .requestee(requestee)
                .chattingRoomId(chattingRoom.getId())
                .status(RoommateRequiredStatus.ACCEPTED)
                .build();
        entityManager.persist(matchingRequired);

        MyRoommate myRoommate = MyRoommate.builder()
                .roommateMatchingRequired(matchingRequired)
                .isDeleted(isDeleted)
                .build();
        entityManager.persist(myRoommate);
        return myRoommate;
    }
}
