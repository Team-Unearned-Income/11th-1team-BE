package org.example.knockin.agreement.repository;

import org.example.knockin.agreement.entity.AgreementLog;
import org.example.knockin.agreement.entity.MemberAgreement;

import java.util.List;

public interface MemberAgreementRepositoryCustom {
    List<MemberAgreement> findByMemberAndAgreementLogNotIn(Long memberId, List<AgreementLog> skipList);
}
