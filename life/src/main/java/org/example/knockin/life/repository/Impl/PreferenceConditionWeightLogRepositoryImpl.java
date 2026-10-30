package org.example.knockin.life.repository.Impl;

import static org.example.knockin.life.entity.QLifePattern.lifePattern;
import static org.example.knockin.life.entity.QPreferenceConditionWeightLog.preferenceConditionWeightLog;
import static org.example.knockin.life.entity.QPreferenceConditionWeightLogDegree.preferenceConditionWeightLogDegree;
import static org.example.knockin.life.entity.QPreferenceConditionWeight.preferenceConditionWeight;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.knockin.life.entity.PreferenceConditionWeightLog;
import org.example.knockin.life.entity.QPreferenceConditionWeightLog;
import org.example.knockin.life.entity.QPreferenceConditionWeightLogDegree;
import org.example.knockin.life.repository.PreferenceConditionWeightLogRepositoryCustom;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PreferenceConditionWeightLogRepositoryImpl implements PreferenceConditionWeightLogRepositoryCustom {

    public final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<PreferenceConditionWeightLog> findLatestLogsWithFetchByMemberId(Long memberId) {
        QPreferenceConditionWeightLog subLog = new QPreferenceConditionWeightLog("subLog");
        QPreferenceConditionWeightLogDegree subDegree = new QPreferenceConditionWeightLogDegree("subDegree");

        return jpaQueryFactory
                .selectFrom(preferenceConditionWeightLog)
                .join(preferenceConditionWeightLog.preferenceConditionWeightLogDegree, preferenceConditionWeightLogDegree).fetchJoin()
                .join(preferenceConditionWeightLog.lifePattern, lifePattern).fetchJoin()
                .join(preferenceConditionWeight)
                .on(
                        preferenceConditionWeight.memberId.eq(preferenceConditionWeightLog.memberId),
                        preferenceConditionWeight.lifePattern.eq(preferenceConditionWeightLog.lifePattern)
                )
                .where(
                        preferenceConditionWeightLog.memberId.eq(memberId),
                        preferenceConditionWeightLogDegree.degree.eq(
                                JPAExpressions
                                        .select(subDegree.degree.max())
                                        .from(subLog)
                                        .join(subLog.preferenceConditionWeightLogDegree, subDegree)
                                        .where(subLog.memberId.eq(memberId))
                        ),
                        lifePattern.isDeleted.isFalse()
                )
                .orderBy(lifePattern.sort.asc())
                .fetch();
    }

}
