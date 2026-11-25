package org.example.knockin.repository.member;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.example.knockin.global.config.QueryDslConfig;
import org.example.knockin.verification.entity.LoginProviderType;
import org.example.knockin.member.entity.BasicInformation;
import org.example.knockin.member.entity.Block;
import org.example.knockin.member.entity.Gender;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberInterest;
import org.example.knockin.member.entity.MemberPrivacy;
import org.example.knockin.member.entity.MemberPrivacyType;
import org.example.knockin.member.entity.MemberRole;
import org.example.knockin.member.entity.MemberState;
import org.example.knockin.member.entity.State;
import org.example.knockin.member.repository.MemberInterestRepository;
import org.example.knockin.member.repository.MemberRepository;
import org.example.knockin.meta.entity.Region;
import org.example.knockin.room.entity.RoomOfferProfile;
import org.example.knockin.member.repository.row.MatchingBasicInfoRow;
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
@DisplayName("회원 관심 목록 Repository")
class MemberRepositoryInterestFilterTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberInterestRepository memberInterestRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("관심 회원 필터는 활성 관심 회원만 후보로 조회한다")
    void findMatchingBasicRowFiltersOnlyActiveInterests() {
        // Given
        Member requester = persistMember("interest-requester");
        Region region = persistRegion("역삼동");
        Member activeCandidate = persistPublicCandidate("active-candidate", "활성회원", region);
        Member deletedCandidate = persistPublicCandidate("deleted-candidate", "해제회원", region);
        Member unrelatedCandidate = persistPublicCandidate("unrelated-candidate", "일반회원", region);
        persistInterest(requester, activeCandidate, false);
        persistInterest(requester, deletedCandidate, true);
        entityManager.flush();
        entityManager.clear();

        // When
        List<MatchingBasicInfoRow> result = memberRepository.findMatchingBasicRow(
                List.of(requester.getId()),
                20,
                requester.getId(),
                requester.getId()
        );

        // Then
        assertThat(result)
                .extracting(MatchingBasicInfoRow::memberId)
                .containsExactly(activeCandidate.getId())
                .doesNotContain(deletedCandidate.getId(), unrelatedCandidate.getId());
    }

    @Test
    @DisplayName("매칭 목록은 요청자와 양방향 활성 차단 관계인 회원을 제외한다")
    void findMatchingBasicRowExcludesBlockedMembersInBothDirections() {
        // Given
        Member requester = persistMember("block-requester");
        Region region = persistRegion("삼성동");
        Member blockedByRequester = persistPublicCandidate("blocked-by-requester", "내가차단", region);
        Member requesterBlockedBy = persistPublicCandidate("requester-blocked-by", "나를차단", region);
        Member deletedBlockCandidate = persistPublicCandidate("deleted-block", "차단해제", region);
        Member visibleCandidate = persistPublicCandidate("visible-candidate", "노출회원", region);
        persistBlock(requester, blockedByRequester, false);
        persistBlock(requesterBlockedBy, requester, false);
        persistBlock(requester, deletedBlockCandidate, true);
        entityManager.flush();
        entityManager.clear();

        // When
        List<MatchingBasicInfoRow> result = memberRepository.findMatchingBasicRow(
                List.of(requester.getId()),
                20,
                null,
                requester.getId()
        );

        // Then
        assertThat(result)
                .extracting(MatchingBasicInfoRow::memberId)
                .containsExactlyInAnyOrder(deletedBlockCandidate.getId(), visibleCandidate.getId())
                .doesNotContain(blockedByRequester.getId(), requesterBlockedBy.getId());
    }

    @Test
    @DisplayName("매칭 목록은 비활성 회원을 제외한다")
    void findMatchingBasicRowExcludesInactiveMembers() {
        // Given
        Member requester = persistMember("state-requester");
        Region region = persistRegion("서초동");
        Member activeCandidate = persistPublicCandidate("active-state-candidate", "활성회원", region);
        Member inactiveCandidate = persistPublicCandidate(
                "inactive-state-candidate", "비활성회원", region, MemberState.INACTIVE);
        entityManager.flush();
        entityManager.clear();

        // When
        List<MatchingBasicInfoRow> result = memberRepository.findMatchingBasicRow(
                List.of(requester.getId()),
                20,
                null,
                requester.getId()
        );

        // Then
        assertThat(result)
                .extracting(MatchingBasicInfoRow::memberId)
                .containsExactly(activeCandidate.getId())
                .doesNotContain(inactiveCandidate.getId());
    }

    @Test
    @DisplayName("상세 관심 여부는 해제된 관심 이력을 활성 상태로 보지 않는다")
    void existsActiveInterestIgnoresDeletedHistory() {
        // Given
        Member requester = persistMember("detail-interest-requester");
        Member activeCandidate = persistMember("detail-active-candidate");
        Member deletedCandidate = persistMember("detail-deleted-candidate");
        persistInterest(requester, activeCandidate, false);
        persistInterest(requester, deletedCandidate, true);
        entityManager.flush();
        entityManager.clear();

        // When & Then
        assertThat(memberInterestRepository.existsBySenderIdAndReceiverIdAndIsDeletedIsFalse(
                requester.getId(), activeCandidate.getId())).isTrue();
        assertThat(memberInterestRepository.existsBySenderIdAndReceiverIdAndIsDeletedIsFalse(
                requester.getId(), deletedCandidate.getId())).isFalse();
    }

    private Member persistPublicCandidate(String providerId, String name, Region region) {
        return persistPublicCandidate(providerId, name, region, MemberState.ACTIVE);
    }

    private Member persistPublicCandidate(
            String providerId,
            String name,
            Region region,
            MemberState memberState
    ) {
        Member member = persistMember(providerId, memberState);
        entityManager.persist(MemberPrivacy.builder()
                .member(member)
                .type(MemberPrivacyType.PUBLIC)
                .build());
        entityManager.persist(BasicInformation.builder()
                .member(member)
                .name(name)
                .birth(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .email(providerId + "@example.com")
                .build());
        entityManager.persist(RoomOfferProfile.builder()
                .memberId(member.getId())
                .regionId(region.getId())
                .deposit(1_000)
                .monthlyRent(50)
                .isComeableAtNegotiable(false)
                .comeableAt(LocalDateTime.of(2026, 8, 1, 9, 0))
                .build());
        return member;
    }

    private Member persistMember(String providerId) {
        return persistMember(providerId, MemberState.ACTIVE);
    }

    private Member persistMember(String providerId, MemberState memberState) {
        Member member = Member.builder()
                .providerType(LoginProviderType.KAKAO)
                .providerId(providerId)
                .role(MemberRole.USER)
                .isDelete(false)
                .build();
        entityManager.persist(member);
        entityManager.persist(State.builder()
                .member(member)
                .states(memberState)
                .build());
        return member;
    }

    private Region persistRegion(String name) {
        Region region = newInstance(Region.class);
        ReflectionTestUtils.setField(region, "name", name);
        ReflectionTestUtils.setField(region, "scope", 3);
        entityManager.persist(region);
        return region;
    }

    private void persistInterest(Member sender, Member receiver, boolean isDeleted) {
        entityManager.persist(MemberInterest.builder()
                .sender(sender)
                .receiver(receiver)
                .isDeleted(isDeleted)
                .build());
    }

    private void persistBlock(Member blocker, Member blocked, boolean isDeleted) {
        entityManager.persist(Block.builder()
                .blocker(blocker)
                .blocked(blocked)
                .isDeleted(isDeleted)
                .build());
    }

    private <T> T newInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                 | NoSuchMethodException exception) {
            throw new IllegalStateException("테스트 엔티티 생성에 실패했습니다.", exception);
        }
    }
}
