package org.example.knockin.agreement.repository;

import java.util.Collection;
import org.example.knockin.agreement.entity.AgreementLog;
import org.example.knockin.agreement.entity.MemberAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberAgreementRepository extends JpaRepository<MemberAgreement, Long>, MemberAgreementRepositoryCustom {
    List<MemberAgreement> findByMemberId(Long memberId);
    List<MemberAgreement> findByMemberIdAndAgreementLogNotIn(Long memberId, Collection<AgreementLog> agreementLogs);
}