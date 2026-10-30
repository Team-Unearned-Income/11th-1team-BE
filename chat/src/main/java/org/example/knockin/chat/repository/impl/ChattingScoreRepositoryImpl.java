package org.example.knockin.chat.repository.impl;

import static org.example.knockin.chat.entity.QChattingScore.chattingScore;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.knockin.chat.entity.ChattingScore;
import org.example.knockin.chat.repository.ChattingScoreRepositoryCustom;
import org.example.knockin.life.entity.QMemberLifePatternLog;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChattingScoreRepositoryImpl implements ChattingScoreRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Optional<ChattingScore> findOneByChattingRequiredIdAndMemberId(Long chattingRequiredId, Long memberId) {
        QMemberLifePatternLog evaluatorLog = new QMemberLifePatternLog("chattingScoreEvaluatorLog");
        return Optional.ofNullable(
                jpaQueryFactory
                        .selectFrom(chattingScore)
                        .where(
                                chattingScore.chattingRequired.id.eq(chattingRequiredId),
                                JPAExpressions.selectOne()
                                        .from(evaluatorLog)
                                        .where(
                                                evaluatorLog.memberLifePatternLogDegree.eq(chattingScore.memberLifePatternLogDegree),
                                                evaluatorLog.memberId.eq(memberId)
                                        )
                                        .exists()
                        )
                        .fetchOne()
        );
    }
}
