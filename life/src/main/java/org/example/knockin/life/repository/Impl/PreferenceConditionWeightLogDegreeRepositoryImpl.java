package org.example.knockin.life.repository.Impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.knockin.life.repository.PreferenceConditionWeightLogDegreeRepositoryCustom;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static org.example.knockin.life.entity.QPreferenceConditionWeightLogDegree.preferenceConditionWeightLogDegree;
import static org.example.knockin.life.entity.QPreferenceConditionWeightLog.preferenceConditionWeightLog;

@Repository
@RequiredArgsConstructor
public class PreferenceConditionWeightLogDegreeRepositoryImpl implements PreferenceConditionWeightLogDegreeRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Optional<Long> findMaxPreferenceConditionWeightLogDegree(Long memberId) {
        return Optional.ofNullable(jpaQueryFactory.select(preferenceConditionWeightLogDegree.degree.max())
                .from(preferenceConditionWeightLogDegree)
                .join(preferenceConditionWeightLog).on(preferenceConditionWeightLog.preferenceConditionWeightLogDegree.eq(preferenceConditionWeightLogDegree))
                .where(preferenceConditionWeightLog.memberId.eq(memberId)).fetchOne());
    }
}
