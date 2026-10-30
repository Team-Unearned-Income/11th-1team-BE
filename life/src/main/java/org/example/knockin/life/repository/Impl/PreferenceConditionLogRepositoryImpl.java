package org.example.knockin.life.repository.Impl;

import static org.example.knockin.life.entity.QLifePattern.lifePattern;
import static org.example.knockin.life.entity.QLifePatternInformation.lifePatternInformation;
import static org.example.knockin.life.entity.QPreferenceConditionLog.preferenceConditionLog;
import static org.example.knockin.life.entity.QPreferenceConditionLogDegree.preferenceConditionLogDegree;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.knockin.life.entity.PreferenceConditionLog;
import org.example.knockin.life.entity.QPreferenceConditionLog;
import org.example.knockin.life.entity.QPreferenceConditionLogDegree;
import org.example.knockin.life.repository.PreferenceConditionLogRepositoryCustom;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PreferenceConditionLogRepositoryImpl implements PreferenceConditionLogRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<PreferenceConditionLog> findLatestLogsWithFetchByMemberId(Long memberId, List<Long> lifePatternInformationIds) {
        if (memberId == null || lifePatternInformationIds == null || lifePatternInformationIds.isEmpty()) {
            return List.of();
        }

        QPreferenceConditionLog subLog = new QPreferenceConditionLog("subLog");
        QPreferenceConditionLogDegree subDegree = new QPreferenceConditionLogDegree("subDegree");

        return jpaQueryFactory
                .selectFrom(preferenceConditionLog)
                .join(preferenceConditionLog.preferenceConditionLogDegree, preferenceConditionLogDegree).fetchJoin()
                .join(preferenceConditionLog.lifePatternInformation, lifePatternInformation).fetchJoin()
                .join(lifePatternInformation.lifePattern, lifePattern).fetchJoin()
                .where(
                        preferenceConditionLog.memberId.eq(memberId),
                        lifePatternInformation.id.in(lifePatternInformationIds),
                        lifePattern.isDeleted.isFalse(),
                        preferenceConditionLogDegree.degree.eq(
                                JPAExpressions
                                        .select(subDegree.degree.max())
                                        .from(subLog)
                                        .join(subLog.preferenceConditionLogDegree, subDegree)
                                        .where(subLog.memberId.eq(memberId))
                        )
                )
                .orderBy(lifePattern.sort.asc(), preferenceConditionLog.id.asc())
                .fetch();
    }

    @Override
    public List<PreferenceConditionLog> findLatestLogsWithFetchByMemberId(Long memberId) {
        QPreferenceConditionLog subLog = new QPreferenceConditionLog("subLog");
        QPreferenceConditionLogDegree subDegree = new QPreferenceConditionLogDegree("subDegree");

        return jpaQueryFactory
                .selectFrom(preferenceConditionLog)
                .join(preferenceConditionLog.preferenceConditionLogDegree, preferenceConditionLogDegree).fetchJoin()
                .join(preferenceConditionLog.lifePatternInformation, lifePatternInformation).fetchJoin()
                .join(lifePatternInformation.lifePattern, lifePattern).fetchJoin()
                .where(
                        preferenceConditionLog.memberId.eq(memberId),
                        preferenceConditionLogDegree.degree.eq(
                                JPAExpressions
                                        .select(subDegree.degree.max())
                                        .from(subLog)
                                        .join(subLog.preferenceConditionLogDegree, subDegree)
                                        .where(subLog.memberId.eq(memberId))
                        ),
                        lifePattern.isDeleted.isFalse()
                )
                .orderBy(lifePattern.sort.asc())
                .fetch();
    }
}
