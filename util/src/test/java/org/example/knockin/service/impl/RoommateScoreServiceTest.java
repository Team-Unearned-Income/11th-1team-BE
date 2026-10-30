package org.example.knockin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.example.knockin.util.dto.Compatibility;
import org.example.knockin.chat.entity.ChattingRequired;
import org.example.knockin.chat.entity.ChattingRequiredStatus;
import org.example.knockin.chat.entity.ChattingScore;
import org.example.knockin.life.entity.LifePattern;
import org.example.knockin.life.entity.LifePatternInformation;
import org.example.knockin.life.entity.LifePatternType;
import org.example.knockin.life.entity.MemberLifePatternLog;
import org.example.knockin.life.entity.MemberLifePatternLogDegree;
import org.example.knockin.life.entity.PreferenceConditionLog;
import org.example.knockin.life.entity.PreferenceConditionLogDegree;
import org.example.knockin.life.entity.PreferenceConditionWeightLog;
import org.example.knockin.life.entity.PreferenceConditionWeightLogDegree;
import org.example.knockin.util.service.impl.JavaRoommateScoreService;
import org.example.knockin.util.service.impl.RoommateScorePolicy;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberRole;
import org.example.knockin.life.repository.LifePatternInformationRepository;
import org.example.knockin.life.repository.MemberLifePatternLogRepository;
import org.example.knockin.life.repository.MemberLifePatternRepository;
import org.example.knockin.life.repository.PreferenceConditionLogRepository;
import org.example.knockin.life.repository.PreferenceConditionRepository;
import org.example.knockin.life.repository.PreferenceConditionWeightLogRepository;
import org.example.knockin.life.repository.PreferenceConditionWeightRepository;
import org.example.knockin.life.repository.row.LifePatternInformationValueRow;
import org.example.knockin.life.repository.row.MatchingLifestyleRow;
import org.example.knockin.life.repository.row.MatchingPreferenceConditionRow;
import org.example.knockin.life.repository.row.MatchingPreferenceConditionWeightRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("룸메이트 궁합 점수 서비스")
class RoommateScoreServiceTest {
    @Mock
    private MemberLifePatternRepository memberLifePatternRepository;

    @Mock
    private PreferenceConditionRepository preferenceConditionRepository;

    @Mock
    private PreferenceConditionWeightRepository preferenceConditionWeightRepository;

    @Mock
    private LifePatternInformationRepository lifePatternInformationRepository;

    @Mock
    private MemberLifePatternLogRepository memberLifePatternLogRepository;

    @Mock
    private PreferenceConditionLogRepository preferenceConditionLogRepository;

    @Mock
    private PreferenceConditionWeightLogRepository preferenceConditionWeightLogRepository;

    private JavaRoommateScoreService roommateScoreService;

    @BeforeEach
    void setUp() {
        RoommateScorePolicy roommateScorePolicy = new RoommateScorePolicy();
        roommateScorePolicy.setPerfectScore(100);
        roommateScorePolicy.setImportantPatternMultiplier(2);

        roommateScoreService = new JavaRoommateScoreService(
                memberLifePatternRepository,
                preferenceConditionRepository,
                preferenceConditionWeightRepository,
                lifePatternInformationRepository,
                roommateScorePolicy,
                memberLifePatternLogRepository,
                preferenceConditionLogRepository,
                preferenceConditionWeightLogRepository
        );
    }

    @Test
    @DisplayName("선호조건, 기본 생활패턴, 중요항목을 반영해 100점 만점 정수 점수를 계산한다")
    void calculateScoresUsesPreferenceFallbackScaleGapAndImportanceBonus() {
        // Given
        Long requesterId = 1L;
        Long targetId = 2L;

        List<MatchingLifestyleRow> lifestyles = List.of(
                lifestyle(requesterId, 11L, 101L, 1001L, "청결 민감도", "3", LifePatternType.SCALE),
                lifestyle(requesterId, 12L, 102L, 2001L, "흡연", "NON_SMOKING", LifePatternType.SINGLE_CHOICE),
                lifestyle(requesterId, 13L, 103L, 3001L, "반려동물", "YES", LifePatternType.SINGLE_CHOICE),
                lifestyle(targetId, 21L, 101L, 1002L, "청결 민감도", "4", LifePatternType.SCALE),
                lifestyle(targetId, 22L, 102L, 2002L, "흡연", "SMOKING", LifePatternType.SINGLE_CHOICE),
                lifestyle(targetId, 23L, 103L, 3001L, "반려동물", "YES", LifePatternType.SINGLE_CHOICE)
        );

        List<MatchingPreferenceConditionRow> conditions = List.of(
                condition(requesterId, 31L, 102L, 2001L, "흡연", "NON_SMOKING", LifePatternType.SINGLE_CHOICE),
                condition(requesterId, 32L, 102L, 2002L, "흡연", "SMOKING", LifePatternType.SINGLE_CHOICE)
        );

        List<MatchingPreferenceConditionWeightRow> weights = List.of(
                new MatchingPreferenceConditionWeightRow(requesterId, 41L, 101L, "청결 민감도", null),
                new MatchingPreferenceConditionWeightRow(requesterId, 42L, 102L, "흡연", null)
        );

        List<Long> lookupMemberIds = List.of(targetId, requesterId);
        when(memberLifePatternRepository.findAllLifestyleByMemberIdIn(lookupMemberIds)).thenReturn(lifestyles);
        when(preferenceConditionRepository.findAllPreferenceConditionByMemberIdIn(lookupMemberIds)).thenReturn(conditions);
        when(preferenceConditionWeightRepository.findAllPreferenceConditionWeightByMemberIdIn(lookupMemberIds)).thenReturn(weights);
        when(lifePatternInformationRepository.findAllValueRowsByLifePatternIdIn(List.of(101L)))
                .thenReturn(List.of(
                        new LifePatternInformationValueRow(101L, "1"),
                        new LifePatternInformationValueRow(101L, "2"),
                        new LifePatternInformationValueRow(101L, "3"),
                        new LifePatternInformationValueRow(101L, "4"),
                        new LifePatternInformationValueRow(101L, "5")
                ));

        // When
        Map<Long, Compatibility> scores = roommateScoreService.calculateScores(requesterId, List.of(targetId));
        Map<Long, Integer> simpleScores = roommateScoreService.calculateSimpleScores(requesterId, List.of(targetId));

        // Then
        assertThat(scores.get(targetId).getTotalScore()).isEqualTo(90);
        assertThat(scores.get(targetId).getLifeStyleInfo())
                .extracting(Compatibility.LifeStyleInfo::getName, Compatibility.LifeStyleInfo::getPercent)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("청결 민감도", 75),
                        org.assertj.core.groups.Tuple.tuple("흡연", 100),
                        org.assertj.core.groups.Tuple.tuple("반려동물", 100)
                );
        assertThat(simpleScores).containsEntry(targetId, 90);
    }

    @Test
    @DisplayName("BOOLEAN 타입은 선호조건 value와 대상 value를 비교해 계산한다")
    void calculateBooleanSimilarityUsesValue() {
        // Given
        Long requesterId = 1L;
        Long targetId = 2L;

        List<MatchingLifestyleRow> lifestyles = List.of(
                lifestyle(requesterId, 11L, 101L, 1001L, "아침형 인간", "true", LifePatternType.BOOLEAN),
                lifestyle(targetId, 21L, 101L, 1002L, "아침형 인간", "false", LifePatternType.BOOLEAN)
        );
        List<MatchingPreferenceConditionRow> conditions = List.of(
                condition(requesterId, 31L, 101L, 1002L, "아침형 인간", "false", LifePatternType.BOOLEAN)
        );

        List<Long> lookupMemberIds = List.of(targetId, requesterId);
        when(memberLifePatternRepository.findAllLifestyleByMemberIdIn(lookupMemberIds)).thenReturn(lifestyles);
        when(preferenceConditionRepository.findAllPreferenceConditionByMemberIdIn(lookupMemberIds)).thenReturn(conditions);
        when(preferenceConditionWeightRepository.findAllPreferenceConditionWeightByMemberIdIn(lookupMemberIds)).thenReturn(List.of());

        // When
        Map<Long, Compatibility> scores = roommateScoreService.calculateScores(requesterId, List.of(targetId));

        // Then
        assertThat(scores.get(targetId).getTotalScore()).isEqualTo(100);
        assertThat(scores.get(targetId).getLifeStyleInfo())
                .extracting(Compatibility.LifeStyleInfo::getName, Compatibility.LifeStyleInfo::getPercent)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("아침형 인간", 100));
    }

    @Test
    @DisplayName("조회자 생활패턴이 없으면 점수를 계산하지 않는다")
    void calculateScoresReturnsEmptyWhenRequesterLifestyleDoesNotExist() {
        // Given
        when(memberLifePatternRepository.findAllLifestyleByMemberIdIn(List.of(2L, 1L))).thenReturn(List.of());
        when(preferenceConditionRepository.findAllPreferenceConditionByMemberIdIn(List.of(2L, 1L))).thenReturn(List.of());
        when(preferenceConditionWeightRepository.findAllPreferenceConditionWeightByMemberIdIn(List.of(2L, 1L))).thenReturn(List.of());

        // When
        Map<Long, Compatibility> scores = roommateScoreService.calculateScores(1L, List.of(2L));

        // Then
        assertThat(scores).isEmpty();
    }

    @Test
    @DisplayName("회원 ID만 받아 필요한 점수 계산 데이터를 벌크 조회한 뒤 점수를 계산한다")
    void calculateScoresByIdsLoadsRowsAndCalculatesScores() {
        // Given
        Long requesterId = 1L;
        Long targetId = 2L;
        List<Long> lookupMemberIds = List.of(targetId, requesterId);

        List<MatchingLifestyleRow> lifestyles = List.of(
                lifestyle(requesterId, 11L, 101L, 1001L, "청결 민감도", "3", LifePatternType.SCALE),
                lifestyle(targetId, 21L, 101L, 1002L, "청결 민감도", "4", LifePatternType.SCALE)
        );

        when(memberLifePatternRepository.findAllLifestyleByMemberIdIn(lookupMemberIds)).thenReturn(lifestyles);
        when(preferenceConditionRepository.findAllPreferenceConditionByMemberIdIn(lookupMemberIds)).thenReturn(List.of());
        when(preferenceConditionWeightRepository.findAllPreferenceConditionWeightByMemberIdIn(lookupMemberIds)).thenReturn(List.of());
        when(lifePatternInformationRepository.findAllValueRowsByLifePatternIdIn(List.of(101L)))
                .thenReturn(List.of(
                        new LifePatternInformationValueRow(101L, "1"),
                        new LifePatternInformationValueRow(101L, "2"),
                        new LifePatternInformationValueRow(101L, "3"),
                        new LifePatternInformationValueRow(101L, "4"),
                        new LifePatternInformationValueRow(101L, "5")
                ));

        // When
        Map<Long, Compatibility> scores = roommateScoreService.calculateScores(requesterId, List.of(targetId));

        // Then
        assertThat(scores.get(targetId).getTotalScore()).isEqualTo(75);
        verify(memberLifePatternRepository).findAllLifestyleByMemberIdIn(lookupMemberIds);
        verify(preferenceConditionRepository).findAllPreferenceConditionByMemberIdIn(lookupMemberIds);
        verify(preferenceConditionWeightRepository).findAllPreferenceConditionWeightByMemberIdIn(lookupMemberIds);
    }

    @Test
    @DisplayName("채팅 요청 기준 양방향 최종점수 row를 각각 하나씩 생성한다")
    void createChattingScoresCreatesBidirectionalRowsAndCalculatesCompatibilityBySavedScores() {
        // Given
        Member requester = member(1L);
        Member requestee = member(2L);
        ChattingRequired chattingRequired = ChattingRequired.builder()
                .requester(requester)
                .requestee(requestee)
                .status(ChattingRequiredStatus.PENDING)
                .build();

        LifePattern smoking = lifePattern(101L, "흡연", LifePatternType.SINGLE_CHOICE, 1);
        LifePattern pet = lifePattern(102L, "반려동물", LifePatternType.SINGLE_CHOICE, 2);

        LifePatternInformation nonSmoking = information(1001L, smoking, "NON_SMOKING");
        LifePatternInformation smokingEveryday = information(1002L, smoking, "SMOKING");
        LifePatternInformation petYes = information(2001L, pet, "YES");

        MemberLifePatternLog requesterSmokingLog = lifePatternLog(requester, nonSmoking);
        MemberLifePatternLog requesterPetLog = lifePatternLog(requester, petYes);
        MemberLifePatternLog requesteeSmokingLog = lifePatternLog(requestee, smokingEveryday);
        MemberLifePatternLog requesteePetLog = lifePatternLog(requestee, petYes);

        PreferenceConditionLog requesterNonSmokingPreference = preferenceLog(requester, nonSmoking);
        PreferenceConditionLog requesterSmokingPreference = preferenceLog(requester, smokingEveryday);
        PreferenceConditionWeightLog requesterSmokingWeight = weightLog(requester, smoking);

        when(memberLifePatternLogRepository.findLatestLogsWithFetchByMemberId(1L))
                .thenReturn(List.of(requesterSmokingLog, requesterPetLog));
        when(memberLifePatternLogRepository.findLatestLogsWithFetchByMemberId(2L))
                .thenReturn(List.of(requesteeSmokingLog, requesteePetLog));
        when(preferenceConditionRepository.findLifeInformationIdByMemberId(1L))
                .thenReturn(List.of(1001L, 1002L));
        when(preferenceConditionRepository.findLifeInformationIdByMemberId(2L))
                .thenReturn(List.of());
        when(preferenceConditionLogRepository.findLatestLogsWithFetchByMemberId(1L, List.of(1001L, 1002L)))
                .thenReturn(List.of(requesterNonSmokingPreference, requesterSmokingPreference));
        when(preferenceConditionWeightLogRepository.findLatestLogsWithFetchByMemberId(1L))
                .thenReturn(List.of(requesterSmokingWeight));
        when(preferenceConditionWeightLogRepository.findLatestLogsWithFetchByMemberId(2L))
                .thenReturn(List.of());
        when(lifePatternInformationRepository.findAllValueRowsByLifePatternIdIn(anyList()))
                .thenReturn(List.of());

        // When
        List<ChattingScore> scores = roommateScoreService.createChattingScores(chattingRequired);
        // Then
        assertThat(scores).hasSize(2);
        assertThat(scores).extracting(ChattingScore::getScore).containsExactly(100, 50);
        assertThat(scores).allSatisfy(score -> assertThat(score.getMemberLifePatternLogDegree()).isNotNull());
    }

    private MatchingLifestyleRow lifestyle(
            Long memberId,
            Long lifestyleId,
            Long lifePatternId,
            Long lifePatternInformationId,
            String name,
            String value,
            LifePatternType type
    ) {
        return new MatchingLifestyleRow(
                memberId,
                lifestyleId,
                lifePatternId,
                lifePatternInformationId,
                name,
                value,
                value,
                type,
                null
        );
    }

    private MatchingPreferenceConditionRow condition(
            Long memberId,
            Long conditionId,
            Long lifePatternId,
            Long lifePatternInformationId,
            String name,
            String value,
            LifePatternType type
    ) {
        return new MatchingPreferenceConditionRow(
                memberId,
                conditionId,
                lifePatternId,
                lifePatternInformationId,
                name,
                value,
                value,
                type,
                null
        );
    }

    private Member member(Long id) {
        return Member.builder()
                .id(id)
                .providerId("provider-" + id)
                .role(MemberRole.USER)
                .build();
    }

    private LifePattern lifePattern(Long id, String name, LifePatternType type, Integer sort) {
        return LifePattern.builder()
                .id(id)
                .name(name)
                .dtype(type)
                .isDeleted(false)
                .sort(sort)
                .build();
    }

    private LifePatternInformation information(Long id, LifePattern lifePattern, String value) {
        return LifePatternInformation.builder()
                .id(id)
                .lifePattern(lifePattern)
                .dvalue(value)
                .description(value)
                .build();
    }

    private MemberLifePatternLog lifePatternLog(Member member, LifePatternInformation information) {
        return MemberLifePatternLog.builder()
                .memberId(member.getId())
                .lifePatternInformation(information)
                .memberLifePatternLogDegree(MemberLifePatternLogDegree.builder().degree(1L).build())
                .build();
    }

    private PreferenceConditionLog preferenceLog(Member member, LifePatternInformation information) {
        return PreferenceConditionLog.builder()
                .memberId(member.getId())
                .lifePatternInformation(information)
                .preferenceConditionLogDegree(PreferenceConditionLogDegree.builder().degree(1L).build())
                .build();
    }

    private PreferenceConditionWeightLog weightLog(Member member, LifePattern lifePattern) {
        return PreferenceConditionWeightLog.builder()
                .memberId(member.getId())
                .lifePattern(lifePattern)
                .preferenceConditionWeightLogDegree(PreferenceConditionWeightLogDegree.builder().degree(1L).build())
                .build();
    }
}
