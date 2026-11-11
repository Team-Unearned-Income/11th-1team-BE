package org.example.knockin.repository.chat;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.example.knockin.chat.repository.ChattingRequiredRepository;
import org.example.knockin.global.config.QueryDslConfig;
import org.example.knockin.verification.entity.LoginProviderType;
import org.example.knockin.chat.entity.ChattingRequired;
import org.example.knockin.chat.entity.ChattingRequiredStatus;
import org.example.knockin.member.entity.BasicInformation;
import org.example.knockin.member.entity.Gender;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberRole;
import org.example.knockin.chat.repository.row.ChatRequestListRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
@DisplayName("채팅 요청 리포지토리")
class ChattingRequiredRepositoryTest {

    private static final LocalDateTime OLDER_REQUEST_CREATED_AT = LocalDateTime.of(2026, 6, 23, 10, 0);
    private static final LocalDateTime LATEST_REQUEST_CREATED_AT = LocalDateTime.of(2026, 6, 23, 11, 0);

    @Autowired
    private ChattingRequiredRepository chattingRequiredRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("역방향 채팅 요청이 이미 있으면 두 회원 사이에 요청이 있다고 조회한다")
    void existsBetweenMembersReturnsTrueWhenReverseRequestExists() {
        // Given
        Member memberA = persistMember("provider-a");
        Member memberB = persistMember("provider-b");
        persistChattingRequired(memberB, memberA);
        entityManager.flush();
        entityManager.clear();

        // When
        boolean exists = chattingRequiredRepository.existsBetweenMembers(memberA.getId(), memberB.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("두 회원 사이의 채팅 요청이 없으면 없다고 조회한다")
    void existsBetweenMembersReturnsFalseWhenRequestDoesNotExist() {
        // Given
        Member memberA = persistMember("provider-a");
        Member memberB = persistMember("provider-b");
        Member otherMember = persistMember("provider-c");
        persistChattingRequired(memberA, otherMember);
        entityManager.flush();
        entityManager.clear();

        // When
        boolean exists = chattingRequiredRepository.existsBetweenMembers(memberA.getId(), memberB.getId());

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("피요청자의 대기 중인 채팅 요청 목록은 요청자 최신 기본 정보를 최신순으로 반환한다")
    void findAllPendingByRequesteeReturnsPendingRequestsWithLatestRequesterInfo() {
        // Given
        Member requestee = persistMember("requestee");
        Member olderRequester = persistMember("older-requester");
        Member latestRequester = persistMember("latest-requester");
        Member acceptedRequester = persistMember("accepted-requester");
        Member otherRequestee = persistMember("other-requestee");

        persistBasicInformation(olderRequester, "이전요청자", LocalDate.of(2001, 1, 1), Gender.MALE);
        persistBasicInformation(latestRequester, "이전이름", LocalDate.of(2002, 2, 2), Gender.MALE);
        persistBasicInformation(latestRequester, "최신요청자", LocalDate.of(2003, 3, 3), Gender.FEMALE);
        persistBasicInformation(requestee, "피요청자", LocalDate.of(1999, 9, 9), Gender.MALE);
        persistBasicInformation(acceptedRequester, "수락요청자", LocalDate.of(2004, 4, 4), Gender.FEMALE);

        ChattingRequired olderRequest = persistChattingRequired(
                olderRequester,
                requestee,
                ChattingRequiredStatus.PENDING,
                OLDER_REQUEST_CREATED_AT
        );
        ChattingRequired latestRequest = persistChattingRequired(
                latestRequester,
                requestee,
                ChattingRequiredStatus.PENDING,
                LATEST_REQUEST_CREATED_AT
        );
        persistChattingRequired(acceptedRequester, requestee, ChattingRequiredStatus.ACCEPTED, LATEST_REQUEST_CREATED_AT.plusMinutes(1));
        persistChattingRequired(latestRequester, otherRequestee, ChattingRequiredStatus.PENDING, LATEST_REQUEST_CREATED_AT.plusMinutes(2));
        entityManager.flush();
        entityManager.clear();

        // When
        List<ChatRequestListRow> rows = chattingRequiredRepository.findAllPendingByRequesteeId(requestee.getId());

        // Then
        assertThat(rows).hasSize(2);

        ChatRequestListRow first = rows.get(0);
        assertThat(first.requiredId()).isEqualTo(latestRequest.getId());
        assertThat(first.status()).isEqualTo(ChattingRequiredStatus.PENDING);
        assertThat(first.memberId()).isEqualTo(latestRequester.getId());
        assertThat(first.memberName()).isEqualTo("최신요청자");
        assertThat(first.birth()).isEqualTo(LocalDate.of(2003, 3, 3));
        assertThat(first.gender()).isEqualTo(Gender.FEMALE);
        assertThat(first.createdAt()).isEqualTo(LATEST_REQUEST_CREATED_AT);

        ChatRequestListRow second = rows.get(1);
        assertThat(second.requiredId()).isEqualTo(olderRequest.getId());
        assertThat(second.status()).isEqualTo(ChattingRequiredStatus.PENDING);
        assertThat(second.memberId()).isEqualTo(olderRequester.getId());
        assertThat(second.memberName()).isEqualTo("이전요청자");
        assertThat(second.birth()).isEqualTo(LocalDate.of(2001, 1, 1));
        assertThat(second.gender()).isEqualTo(Gender.MALE);
        assertThat(second.createdAt()).isEqualTo(OLDER_REQUEST_CREATED_AT);
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

    private ChattingRequired persistChattingRequired(Member requester, Member requestee) {
        return persistChattingRequired(requester, requestee, ChattingRequiredStatus.ACCEPTED);
    }

    private ChattingRequired persistChattingRequired(Member requester, Member requestee, ChattingRequiredStatus status) {
        ChattingRequired chattingRequired = ChattingRequired.builder()
                .requesterId(requester.getId())
                .requesteeId(requestee.getId())
                .status(status)
                .build();
        entityManager.persist(chattingRequired);
        return chattingRequired;
    }

    private ChattingRequired persistChattingRequired(
            Member requester,
            Member requestee,
            ChattingRequiredStatus status,
            LocalDateTime createdAt
    ) {
        ChattingRequired chattingRequired = persistChattingRequired(requester, requestee, status);
        entityManager.flush();
        entityManager.createNativeQuery("update chatting_required set created_at = ? where id = ?")
                .setParameter(1, Timestamp.valueOf(createdAt))
                .setParameter(2, chattingRequired.getId())
                .executeUpdate();
        return chattingRequired;
    }

    private BasicInformation persistBasicInformation(Member member, String name, LocalDate birth, Gender gender) {
        BasicInformation basicInformation = BasicInformation.builder()
                .member(member)
                .name(name)
                .birth(birth)
                .gender(gender)
                .email(name + "@example.com")
                .build();
        entityManager.persist(basicInformation);
        return basicInformation;
    }
}
