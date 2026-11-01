package org.example.knockin.agreement.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.example.knockin.agreement.dto.BoTermsDetailDto;
import org.example.knockin.agreement.dto.BoTermsDto;
import org.example.knockin.agreement.dto.BoTermsListDto;
import org.example.knockin.agreement.dto.BoTypeTermsDto;
import org.example.knockin.agreement.dto.BoTypeTermsListDto;
import org.example.knockin.agreement.dto.TermsDetailDto;
import org.example.knockin.agreement.dto.TermsListDto;
import org.example.knockin.agreement.entity.Agreement;
import org.example.knockin.agreement.entity.AgreementLog;
import org.example.knockin.agreement.entity.AgreementType;
import org.example.knockin.agreement.entity.MemberAgreement;
import org.example.knockin.agreement.repository.MemberAgreementRepository;
import org.example.knockin.global.exception.AgreementErrorCode;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.agreement.repository.AgreementLogRepository;
import org.example.knockin.agreement.repository.AgreementRepository;
import org.example.knockin.agreement.repository.AgreementTypeRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgreementServiceImpl {
    private final AgreementRepository agreementRepository;
    private final AgreementLogRepository agreementLogRepository;
    private final AgreementTypeRepository agreementTypeRepository;
    private final MemberAgreementRepository memberAgreementRepository;

    @Transactional
    public Agreement saveAgreement(BoTermsDto.Request request) {
        AgreementType agreementType = findAgreementTypeById(request.getAgreementTypeId());
        Agreement agreement = Agreement.builder()
                .title(request.getTitle())
                .contents(request.getContents())
                .isRequired(request.getIsRequired())
                .type(agreementType)
                .build();

        Agreement agreementEntity = agreementRepository.save(agreement);
        agreementLogRepository.save(AgreementLog.builder().agreement(agreementEntity).isCurrent(true).build());
        return agreementEntity;
    }

    public AgreementType findAgreementType(Long agreementId) {
        return agreementRepository.findById(agreementId).orElseThrow(() -> new BusinessException(AgreementErrorCode.AGREEMENT_NOT_FOUNT)).getType();
    }

    @Transactional
    public Agreement modifyTemporaryAgreement(BoTermsDto.Request request, Long termsId) {
        AgreementType type = findAgreementType(termsId);

        Agreement agreement = Agreement.builder()
                .title(request.getTitle())
                .contents(request.getContents())
                .isRequired(request.getIsRequired())
                .type(type)
                .isDeleted(false)
                .build();

        Agreement agreementEntity = agreementRepository.save(agreement);
        agreementLogRepository.save(AgreementLog.builder().agreement(agreementEntity).isCurrent(false).build());
        return agreementEntity;
    }

    @Transactional
    public Agreement modifyAgreement(BoTermsDto.Request request, Long agreementId) {
        Agreement agreementEntity = agreementRepository.findById(agreementId).orElseThrow(() -> new BusinessException(AgreementErrorCode.AGREEMENT_NOT_FOUNT));
        List<AgreementLog> agreementLogs = agreementLogRepository.findByAgreementIn(agreementRepository.findByType(agreementEntity.getType()));
        AgreementLog targetLog = agreementLogs.stream()
                .filter(log -> agreementId.equals(log.getAgreement().getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(AgreementErrorCode.AGREEMENT_LOG_NOT_FOUNT));
        agreementLogs.forEach(AgreementLog::clearCurrent);

        Agreement newAgreement = Agreement.builder()
                .title(request.getTitle())
                .contents(request.getContents())
                .isRequired(request.getIsRequired())
                .build();

        agreementEntity.modifyAgreement(newAgreement);
        targetLog.enableCurrent();
        return agreementEntity;
    }

    public List<BoTermsListDto.Response.TermsItem> findAgreementList(Pageable pageable, Long agreementTypeId) {
        return agreementLogRepository.findByAgreemnetIsCurrent(true, pageable, agreementTypeId).stream().map(item ->
                BoTermsListDto.Response.TermsItem.builder().title(item.getAgreement().getTitle()).createAt(item.getAgreement().getCreatedAt()).id(item.getAgreement().getId()).isCurrent(item.getIsCurrent()).build()).toList();
    }

    public BoTermsDetailDto.Response findBoAgreement(Long id) {
        Agreement agreement = agreementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(AgreementErrorCode.AGREEMENT_NOT_FOUNT));
        return BoTermsDetailDto.Response.builder()
                .id(agreement.getId())
                .title(agreement.getTitle())
                .contents(agreement.getContents())
                .createAt(agreement.getCreatedAt())
                .build();
    }

    public TermsDetailDto.Response findUserAgreement(Long id) {
        Agreement agreement = agreementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(AgreementErrorCode.AGREEMENT_NOT_FOUNT));
        return TermsDetailDto.Response.builder()
                .id(agreement.getId())
                .contents(agreement.getContents())
                .build();
    }

    @Transactional
    public Agreement deleteAgreement(Long id) {
        Agreement agreement = agreementRepository.findById(id).orElseThrow(() -> new BusinessException(AgreementErrorCode.AGREEMENT_NOT_FOUNT));
        agreement.deleteAgreement();
        return agreement;
    }

    public AgreementType findAgreementTypeById(Long id) {
        return agreementTypeRepository.findById(id).orElseThrow(() -> new BusinessException(AgreementErrorCode.AGREEMENT_TYPE_NOT_FOUNT));
    }

    @Transactional
    public void modifyAgreementType(Long termTypeId, BoTypeTermsDto.Request request) {
        AgreementType type = findAgreementTypeById(termTypeId);
        type.modifyAgreementType(request.getTitle());
    }

    public List<BoTypeTermsListDto.Response.TermsTypeItem> findTypeTermsList() {
        return agreementTypeRepository.findAll().stream().map(item ->
                BoTypeTermsListDto.Response.TermsTypeItem.builder().id(item.getId()).title(item.getName()).createAt(item.getCreatedAt()).build()).toList();
    }

    @Transactional
    public AgreementType saveTermType(BoTypeTermsDto.Request request) {
        AgreementType type = AgreementType.builder().name(request.getTitle()).isDeleted(false).build();
        return agreementTypeRepository.save(type);
    }

    @Transactional
    public AgreementType deleteTermType(Long termTypeId) {
        AgreementType agreementType = agreementTypeRepository.findById(termTypeId).orElseThrow(() -> new BusinessException(AgreementErrorCode.AGREEMENT_TYPE_NOT_FOUNT));
        agreementType.deleteAgreementType();
        return agreementType;
    }

    public List<TermsListDto.Response.TermsItem> findByAgreementsIsCurrentAndIsDeleted() {
        return agreementRepository.findByAgreementsIsCurrentAndIsDeleted().stream().map(item ->
                TermsListDto.Response.TermsItem.builder().id(item.getId()).title(item.getTitle()).build()).toList();
    }

    public List<AgreementLog> findByAgreementLogIsCurrent(List<Long> agreementIds) {
        return agreementLogRepository.findByAgreementLogIsCurrent(agreementIds);
    }

    @Transactional
    public boolean saveMemberAgreement(List<Long> agreementIds, Long memberId) {
        return saveAllMemberAgreement(agreementIds, memberId).stream()
                .noneMatch(agreement -> Boolean.TRUE.equals(agreement.getIsAgreed()));
    }

    @Transactional
    public List<MemberAgreement> saveAllMemberAgreement(List<Long> agreementIds, Long memberId) {
        List<MemberAgreement> existing = findByMemberId(memberId);
        List<AgreementLog> requestedLogs = findByAgreementLogIsCurrent(agreementIds);
        if (!existing.isEmpty()) {
            return synchronizeMemberAgreements(requestedLogs, existing, memberId);
        }
        List<MemberAgreement> memberAgreementList = requestedLogs.stream()
                .map(log -> newMemberAgreement(memberId, log))
                .toList();
        return memberAgreementRepository.saveAll(memberAgreementList);
    }

    @Transactional
    public void modifyMemberAgreement(List<Long> agreementIds, Long memberId) {
        synchronizeMemberAgreements(findByAgreementLogIsCurrent(agreementIds), findByMemberId(memberId), memberId);
    }

    private List<MemberAgreement> synchronizeMemberAgreements(List<AgreementLog> requestedLogs, List<MemberAgreement> existing, Long memberId) {
        Set<Long> requestedLogIds = requestedLogs.stream().map(AgreementLog::getId).collect(Collectors.toSet());
        Set<Long> existingLogIds = new HashSet<>();
        existing.forEach(agreement -> {
            Long logId = agreement.getAgreementLog().getId();
            existingLogIds.add(logId);
            if (requestedLogIds.contains(logId)) {
                agreement.enableAgree();
            } else {
                agreement.disableAgree();
            }
        });

        List<MemberAgreement> result = new ArrayList<>(existing);
        requestedLogs.stream()
                .filter(log -> existingLogIds.add(log.getId()))
                .forEach(log -> result.add(saveMemberAgreement(memberId, log)));
        return result;
    }

    @Transactional
    public MemberAgreement saveMemberAgreement(Long memberId, AgreementLog agreementLog) {
        return memberAgreementRepository.save(newMemberAgreement(memberId, agreementLog));
    }

    private MemberAgreement newMemberAgreement(Long memberId, AgreementLog agreementLog) {
        return MemberAgreement.builder()
                .memberId(memberId)
                .agreementLog(agreementLog)
                .isAgreed(true)
                .build();
    }

    public List<MemberAgreement> findByMemberId(Long memberId) {
        return memberAgreementRepository.findByMemberId(memberId);
    }

}
