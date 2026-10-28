package org.example.knockin.agreement.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.knockin.agreement.entity.AgreementLog;
import org.example.knockin.agreement.entity.MemberAgreement;
import org.example.knockin.agreement.repository.MemberAgreementRepositoryCustom;
import org.springframework.stereotype.Repository;

import java.util.List;
import static org.example.knockin.agreement.entity.QMemberAgreement.memberAgreement;

@Repository
@RequiredArgsConstructor
public class MemberAgreementRepositoryImpl implements MemberAgreementRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<MemberAgreement> findByMemberAndAgreementLogNotIn(Long memberId, List<AgreementLog> skipList) {
        return jpaQueryFactory.selectFrom(memberAgreement).where(memberAgreement.memberId.eq(memberId), memberAgreement.agreementLog.notIn(skipList)).fetch();
    }
}
