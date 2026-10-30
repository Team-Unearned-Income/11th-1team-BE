package org.example.knockin.agreement.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.knockin.agreement.entity.AgreementLog;
import org.example.knockin.agreement.entity.MemberAgreement;
import org.example.knockin.agreement.repository.MemberAgreementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberAgreementServiceImpl {
    private final MemberAgreementRepository memberAgreementRepository;

    @Transactional
    public List<MemberAgreement> saveAll(List<MemberAgreement> memberAgreementList) {
        return memberAgreementRepository.saveAll(memberAgreementList);
    }

    @Transactional
    public MemberAgreement save(MemberAgreement memberAgreement) {
        return memberAgreementRepository.save(memberAgreement);
    }

    public List<MemberAgreement> findByMemberId(Long memberId) {
        return memberAgreementRepository.findByMemberId(memberId);
    }

    public List<MemberAgreement> findByMemberIdAndAgreementLogNotIn(Long memberId, List<AgreementLog> skipList) {
        return memberAgreementRepository.findByMemberIdAndAgreementLogNotIn(memberId, skipList);
    }
}
