package org.example.knockin.repository.room;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.example.knockin.global.config.QueryDslConfig;
import org.example.knockin.verification.entity.LoginProviderType;
import org.example.knockin.chat.entity.ChattingRequired;
import org.example.knockin.chat.entity.ChattingRequiredStatus;
import org.example.knockin.chat.entity.ChattingRoom;
import org.example.knockin.chat.entity.ChattingScore;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberRole;
import org.example.knockin.mate.entity.MyRoommate;
import org.example.knockin.mate.entity.RoommateMatchingRequired;
import org.example.knockin.mate.entity.RoommateRequiredStatus;
import org.example.knockin.mate.entity.RoommateScore;
import org.example.knockin.life.entity.LifePattern;
import org.example.knockin.life.entity.LifePatternInformation;
import org.example.knockin.life.entity.LifePatternType;
import org.example.knockin.life.entity.MemberLifePatternLog;
import org.example.knockin.life.entity.MemberLifePatternLogDegree;
import org.example.knockin.life.entity.PreferenceConditionWeightLog;
import org.example.knockin.mate.repository.RoommateScoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
@DisplayName("룸메이트 점수 리포지토리")
class RoommateScoreRepositoryTest {

    @Autowired
    private RoommateScoreRepository roommateScoreRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("내 룸메이트 점수 조회는 연결된 채팅 점수의 로그 차수로 평가자 방향을 판별한다")
    void findOneByMyRoommateIdAndMemberIdReturnsEvaluatorDirection() {
        // Given
        Member evaluator = persistMember("score-evaluator");
        Member target = persistMember("score-target");
        MyRoommate myRoommate = persistMyRoommate(evaluator, target);
        LifePattern lifePattern = persistLifePattern("청결 민감도", 1);
        LifePatternInformation information = persistLifePatternInformation(lifePattern, "3");
        MemberLifePatternLogDegree evaluatorDegree = persistMemberLifePatternLogDegree(1L);
        MemberLifePatternLogDegree targetDegree = persistMemberLifePatternLogDegree(1L);
        persistMemberLifePatternLog(evaluator, information, evaluatorDegree);
        persistMemberLifePatternLog(target, information, targetDegree);
        ChattingRequired chattingRequired = myRoommate.getRoommateMatchingRequired()
                .getChattingRoom()
                .getChattingRequired();
        ChattingScore evaluatorScore = persistChattingScore(chattingRequired, evaluatorDegree, 80);
        ChattingScore targetScore = persistChattingScore(chattingRequired, targetDegree, 20);
        persistRoommateScore(myRoommate, evaluatorScore);
        persistRoommateScore(myRoommate, targetScore);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<RoommateScore> score = roommateScoreRepository.findOneByMyRoommateIdAndMemberId(
                myRoommate.getId(),
                evaluator.getId()
        );

        // Then
        assertThat(score).isPresent();
        assertThat(score.orElseThrow().getChattingScore().getScore()).isEqualTo(80);
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
        ChattingRequired chattingRequired = ChattingRequired.builder()
                .requester(requester)
                .requestee(requestee)
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
                .chattingRoom(chattingRoom)
                .status(RoommateRequiredStatus.ACCEPTED)
                .build();
        entityManager.persist(matchingRequired);

        MyRoommate myRoommate = MyRoommate.builder()
                .roommateMatchingRequired(matchingRequired)
                .isDeleted(false)
                .build();
        entityManager.persist(myRoommate);
        return myRoommate;
    }

    private LifePattern persistLifePattern(String name, Integer sort) {
        LifePattern lifePattern = LifePattern.builder()
                .name(name)
                .dtype(LifePatternType.SCALE)
                .isDeleted(false)
                .sort(sort)
                .lifePatternDescription("생활패턴 설명")
                .preferenceDescription("선호조건 설명")
                .build();
        entityManager.persist(lifePattern);
        return lifePattern;
    }

    private LifePatternInformation persistLifePatternInformation(LifePattern lifePattern, String value) {
        LifePatternInformation information = LifePatternInformation.builder()
                .lifePattern(lifePattern)
                .dvalue(value)
                .description(value)
                .build();
        entityManager.persist(information);
        return information;
    }

    private MemberLifePatternLogDegree persistMemberLifePatternLogDegree(Long degree) {
        MemberLifePatternLogDegree logDegree = MemberLifePatternLogDegree.builder().degree(degree).build();
        entityManager.persist(logDegree);
        return logDegree;
    }

    private MemberLifePatternLog persistMemberLifePatternLog(
            Member member,
            LifePatternInformation information,
            MemberLifePatternLogDegree logDegree
    ) {
        MemberLifePatternLog log = MemberLifePatternLog.builder()
                .memberId(member.getId())
                .lifePatternInformation(information)
                .memberLifePatternLogDegree(logDegree)
                .build();
        entityManager.persist(log);
        return log;
    }

    private PreferenceConditionWeightLog persistPreferenceConditionWeightLog(Member member, LifePattern lifePattern) {
        PreferenceConditionWeightLog log = PreferenceConditionWeightLog.builder()
                .memberId(member.getId())
                .lifePattern(lifePattern)
                .build();
        entityManager.persist(log);
        return log;
    }

    private ChattingScore persistChattingScore(
            ChattingRequired chattingRequired,
            MemberLifePatternLogDegree memberLifePatternLogDegree,
            Integer score
    ) {
        ChattingScore chattingScore = ChattingScore.builder()
                .chattingRequired(chattingRequired)
                .memberLifePatternLogDegree(memberLifePatternLogDegree)
                .score(score)
                .build();
        entityManager.persist(chattingScore);
        return chattingScore;
    }

    private void persistRoommateScore(MyRoommate myRoommate, ChattingScore chattingScore) {
        RoommateScore roommateScore = RoommateScore.builder()
                .myRoommate(myRoommate)
                .chattingScore(chattingScore)
                .build();
        entityManager.persist(roommateScore);
    }

}
