package org.example.knockin.agreement.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.example.knockin.agreement.dto.BoTermsDto;
import org.example.knockin.agreement.dto.BoTypeTermsDto;
import org.example.knockin.agreement.entity.Agreement;
import org.example.knockin.agreement.entity.AgreementLog;
import org.example.knockin.agreement.entity.AgreementType;
import org.example.knockin.agreement.entity.MemberAgreement;
import org.example.knockin.agreement.repository.AgreementLogRepository;
import org.example.knockin.agreement.repository.AgreementRepository;
import org.example.knockin.agreement.repository.AgreementTypeRepository;
import org.example.knockin.agreement.repository.MemberAgreementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@ContextConfiguration(classes = AgreementServicePersistenceTest.AgreementTestConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("약관 모듈 서비스 영속성 검증")
class AgreementServicePersistenceTest {

    @Autowired
    private AgreementServiceImpl agreementService;

    @Autowired
    private AgreementRepository agreementRepository;

    @Autowired
    private AgreementLogRepository agreementLogRepository;

    @Autowired
    private AgreementTypeRepository agreementTypeRepository;

    @Autowired
    private MemberAgreementRepository memberAgreementRepository;

    @BeforeEach
    void cleanDatabase() {
        memberAgreementRepository.deleteAllInBatch();
        agreementLogRepository.deleteAllInBatch();
        agreementRepository.deleteAllInBatch();
        agreementTypeRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("호출자의 트랜잭션 없이 수정한 약관 유형명이 다음 조회에도 반영된다")
    void modifiesAgreementTypeInItsOwnWriteTransaction() {
        AgreementType type = saveAgreementType();

        agreementService.modifyAgreementType(type.getId(), new BoTypeTermsDto.Request("변경된 약관 유형"));

        assertThat(agreementTypeRepository.findById(type.getId()).orElseThrow().getName())
                .isEqualTo("변경된 약관 유형");
    }

    @Test
    @DisplayName("철회 후 재동의는 기존 행을 다시 활성화하고 다른 회원의 동의는 유지한다")
    void reusesConsentWhenMemberAgreesAgainWithoutChangingAnotherMember() {
        Agreement agreement = agreementService.saveAgreement(termsRequest(saveAgreementType(), "이용약관", "약관 내용"));
        Long memberId = 10L;
        Long otherMemberId = 20L;
        List<Long> requestedTerms = List.of(agreement.getId());

        assertThat(agreementService.saveMemberAgreement(requestedTerms, memberId)).isFalse();
        assertThat(agreementService.saveMemberAgreement(requestedTerms, otherMemberId)).isFalse();
        MemberAgreement originalConsent = consentFor(memberId);
        Long originalConsentId = originalConsent.getId();

        agreementService.modifyMemberAgreement(List.of(), memberId);

        assertThat(consentFor(memberId).getIsAgreed()).isFalse();
        assertThat(consentFor(otherMemberId).getIsAgreed()).isTrue();

        assertThat(agreementService.saveMemberAgreement(requestedTerms, memberId)).isFalse();

        MemberAgreement reactivatedConsent = consentFor(memberId);
        assertThat(reactivatedConsent.getId()).isEqualTo(originalConsentId);
        assertThat(reactivatedConsent.getIsAgreed()).isTrue();
        assertThat(consentFor(otherMemberId).getIsAgreed()).isTrue();
        assertThat(memberAgreementRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("약관과 이력의 ID가 달라도 상세 조회와 발행은 지정된 약관을 처리한다")
    void findsAndPublishesAgreementWhenLogHasDifferentId() {
        AgreementType type = saveAgreementType();
        Agreement previous = agreementService.saveAgreement(termsRequest(type, "기존 약관", "기존 내용"));
        Agreement draft = agreementService.modifyTemporaryAgreement(
                termsRequest(type, "임시 약관", "임시 내용"), previous.getId());
        AgreementLog draftLog = agreementLogRepository.findByAgreementIdAndIsCurrent(draft.getId(), false)
                .orElseThrow();
        if (draft.getId().equals(draftLog.getId())) {
            agreementLogRepository.deleteById(draftLog.getId());
            draftLog = agreementLogRepository.save(AgreementLog.builder().agreement(draft).isCurrent(false).build());
        }
        Long draftLogId = draftLog.getId();
        assertThat(draftLogId).isNotEqualTo(draft.getId());

        assertThat(agreementService.findBoAgreement(draft.getId()).getTitle()).isEqualTo("임시 약관");
        assertThat(agreementService.findUserAgreement(draft.getId()).getContents()).isEqualTo("임시 내용");

        agreementService.modifyAgreement(termsRequest(type, "발행된 약관", "발행된 내용"), draft.getId());

        Agreement published = agreementRepository.findById(draft.getId()).orElseThrow();
        assertThat(published.getTitle()).isEqualTo("발행된 약관");
        assertThat(published.getContents()).isEqualTo("발행된 내용");
        assertThat(agreementLogRepository.findById(draftLogId).orElseThrow().getIsCurrent()).isTrue();
        assertThat(agreementLogRepository.findByAgreementIdAndIsCurrent(previous.getId(), true)).isEmpty();
        assertThat(agreementRepository.findById(previous.getId()).orElseThrow().getTitle()).isEqualTo("기존 약관");
    }

    private AgreementType saveAgreementType() {
        return agreementTypeRepository.save(AgreementType.builder().name("약관 유형").isDeleted(false).build());
    }

    private BoTermsDto.Request termsRequest(AgreementType type, String title, String contents) {
        return new BoTermsDto.Request(type.getId(), title, contents, true);
    }

    private MemberAgreement consentFor(Long memberId) {
        List<MemberAgreement> agreements = memberAgreementRepository.findByMemberId(memberId);
        assertThat(agreements).hasSize(1);
        return agreements.getFirst();
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = Agreement.class)
    @EnableJpaRepositories(basePackageClasses = AgreementRepository.class)
    @Import(AgreementServiceImpl.class)
    static class AgreementTestConfiguration {

        @Bean
        JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
            return new JPAQueryFactory(entityManager);
        }
    }
}
