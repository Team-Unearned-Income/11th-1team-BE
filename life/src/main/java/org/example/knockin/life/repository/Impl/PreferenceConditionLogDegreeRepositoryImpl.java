package org.example.knockin.life.repository.Impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.knockin.life.repository.PreferenceConditionLogDegreeRepositoryCustom;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static org.example.knockin.life.entity.QPreferenceConditionLogDegree.preferenceConditionLogDegree;
import static org.example.knockin.life.entity.QPreferenceConditionLog.preferenceConditionLog;

@Repository
@RequiredArgsConstructor
public class PreferenceConditionLogDegreeRepositoryImpl implements PreferenceConditionLogDegreeRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Optional<Long> findMaxPreferenceConditionLogDegree(Long memberId) {
        return Optional.ofNullable(jpaQueryFactory.select(preferenceConditionLogDegree.degree.max())
                .from(preferenceConditionLogDegree)
                .join(preferenceConditionLog).on(preferenceConditionLog.preferenceConditionLogDegree.eq(preferenceConditionLogDegree))
                .where(preferenceConditionLog.memberId.eq(memberId)).fetchOne());
    }
}
