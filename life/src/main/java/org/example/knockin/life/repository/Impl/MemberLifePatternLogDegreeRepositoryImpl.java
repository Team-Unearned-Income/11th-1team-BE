package org.example.knockin.life.repository.Impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.knockin.life.repository.MemberLifePatternLogDegreeRepositoryCustom;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static org.example.knockin.life.entity.QMemberLifePatternLogDegree.memberLifePatternLogDegree;
import static org.example.knockin.life.entity.QMemberLifePatternLog.memberLifePatternLog;

@Repository
@RequiredArgsConstructor
public class MemberLifePatternLogDegreeRepositoryImpl implements MemberLifePatternLogDegreeRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Optional<Long> findMaxmemberLifePatternLogDegree(Long memberId) {
        return Optional.ofNullable(jpaQueryFactory.select(memberLifePatternLogDegree.degree.max())
                .from(memberLifePatternLogDegree)
                .join(memberLifePatternLog).on(memberLifePatternLog.memberLifePatternLogDegree.eq(memberLifePatternLogDegree))
                .where(memberLifePatternLog.memberId.eq(memberId)).fetchOne());
    }
}
