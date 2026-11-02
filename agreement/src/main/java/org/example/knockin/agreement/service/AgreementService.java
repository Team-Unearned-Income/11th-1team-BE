package org.example.knockin.agreement.service;

import java.util.List;
import org.example.knockin.agreement.dto.BoTermsDetailDto;
import org.example.knockin.agreement.dto.BoTermsDto;
import org.example.knockin.agreement.dto.BoTermsListDto.Response.TermsItem;
import org.example.knockin.agreement.dto.BoTypeTermsDto;
import org.example.knockin.agreement.dto.BoTypeTermsListDto;
import org.example.knockin.agreement.dto.TermsDetailDto;
import org.example.knockin.agreement.dto.TermsListDto;
import org.example.knockin.agreement.entity.Agreement;
import org.example.knockin.agreement.entity.AgreementLog;
import org.example.knockin.agreement.entity.AgreementType;
import org.example.knockin.agreement.entity.MemberAgreement;
import org.springframework.data.domain.Pageable;

public interface AgreementService {
    Agreement saveAgreement(BoTermsDto.Request request);

    AgreementType findAgreementType(Long agreementId);

    Agreement modifyTemporaryAgreement(BoTermsDto.Request request, Long termsId);

    Agreement modifyAgreement(BoTermsDto.Request request, Long agreementId);

    List<TermsItem> findAgreementList(Pageable pageable, Long agreementTypeId);

    BoTermsDetailDto.Response findBoAgreement(Long id);

    TermsDetailDto.Response findUserAgreement(Long id);

    Agreement deleteAgreement(Long id);

    AgreementType findAgreementTypeById(Long id);

    void modifyAgreementType(Long termTypeId, BoTypeTermsDto.Request request);

    List<BoTypeTermsListDto.Response.TermsTypeItem> findTypeTermsList();

    AgreementType saveTermType(BoTypeTermsDto.Request request);

    AgreementType deleteTermType(Long termTypeId);

    List<TermsListDto.Response.TermsItem> findByAgreementsIsCurrentAndIsDeleted();

    List<AgreementLog> findByAgreementLogIsCurrent(List<Long> agreementIds);

    boolean saveMemberAgreement(List<Long> agreementIds, Long memberId);

    List<MemberAgreement> saveAllMemberAgreement(List<Long> agreementIds, Long memberId);

    void modifyMemberAgreement(List<Long> agreementIds, Long memberId);

    List<MemberAgreement> synchronizeMemberAgreements(List<AgreementLog> requestedLogs, List<MemberAgreement> existing, Long memberId);

    MemberAgreement saveMemberAgreement(Long memberId, AgreementLog agreementLog);

    MemberAgreement newMemberAgreement(Long memberId, AgreementLog agreementLog);

    List<MemberAgreement> findByMemberId(Long memberId);
}
