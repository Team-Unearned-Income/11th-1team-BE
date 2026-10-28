package org.example.knockin.mate.repository.impl;

import static org.example.knockin.mate.entity.QRoommateScore.roommateScore;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.knockin.life.entity.QMemberLifePatternLog;
import org.example.knockin.mate.entity.RoommateScore;
import org.example.knockin.mate.repository.RoommateScoreRepositoryCustom;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RoommateScoreRepositoryImpl implements RoommateScoreRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Optional<RoommateScore> findOneByMyRoommateIdAndMemberId(Long myRoommateId, Long memberId) {
        QMemberLifePatternLog evaluatorLog = new QMemberLifePatternLog("roommateScoreEvaluatorLog");
        return Optional.ofNullable(
                jpaQueryFactory
                        .selectFrom(roommateScore)
                        .where(
                                roommateScore.myRoommate.id.eq(myRoommateId),
                                JPAExpressions.selectOne()
                                        .from(evaluatorLog)
                                        .where(
                                                evaluatorLog.memberLifePatternLogDegree.eq(
                                                        roommateScore.chattingScore.memberLifePatternLogDegree
                                                ),
                                                evaluatorLog.memberId.eq(memberId)
                                        )
                                        .exists()
                        )
                        .fetchOne()
        );
    }
}
