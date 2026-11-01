package org.example.knockin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
import org.example.knockin.agreement.repository.AgreementLogRepository;
import org.example.knockin.agreement.repository.AgreementRepository;
import org.example.knockin.agreement.repository.AgreementTypeRepository;
import org.example.knockin.agreement.repository.MemberAgreementRepository;
import org.example.knockin.agreement.service.impl.AgreementServiceImpl;
import org.example.knockin.global.exception.AgreementErrorCode;
import org.example.knockin.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("약관 관리 서비스")
class AgreementServiceImplTest {

    @Mock
    private AgreementRepository agreementRepository;

    @Mock
    private AgreementLogRepository agreementLogRepository;

    @Mock
    private AgreementTypeRepository agreementTypeRepository;

    @Mock
    private MemberAgreementRepository memberAgreementRepository;

    @InjectMocks
    private AgreementServiceImpl agreementService;

    @Test
    @DisplayName("등록 요청의 약관 유형과 내용을 저장하고 저장된 약관의 현행 이력을 생성한다")
    void saveAgreementCreatesCurrentLogForSavedAgreement() {
        AgreementType type = agreementType(1L);
        BoTermsDto.Request request = new BoTermsDto.Request(1L, "약관 제목", "약관 내용", true);
        Agreement saved = Agreement.builder().id(10L).build();
        given(agreementTypeRepository.findById(1L)).willReturn(Optional.of(type));
        given(agreementRepository.save(any(Agreement.class))).willReturn(saved);

        Agreement result = agreementService.saveAgreement(request);

        ArgumentCaptor<Agreement> agreementCaptor = ArgumentCaptor.forClass(Agreement.class);
        verify(agreementRepository).save(agreementCaptor.capture());
        assertThat(agreementCaptor.getValue()).satisfies(agreement -> {
            assertThat(agreement.getType()).isSameAs(type);
            assertThat(agreement.getTitle()).isEqualTo("약관 제목");
            assertThat(agreement.getContents()).isEqualTo("약관 내용");
            assertThat(agreement.getIsRequired()).isTrue();
            assertThat(agreement.getIsDeleted()).isFalse();
        });
        ArgumentCaptor<AgreementLog> logCaptor = ArgumentCaptor.forClass(AgreementLog.class);
        verify(agreementLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getAgreement()).isSameAs(saved);
        assertThat(logCaptor.getValue().getIsCurrent()).isTrue();
        assertThat(result).isSameAs(saved);
    }

    @Test
    @DisplayName("없는 약관 유형으로 등록하면 약관과 이력을 저장하지 않는다")
    void saveAgreementRejectsUnknownType() {
        given(agreementTypeRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> agreementService.saveAgreement(
                new BoTermsDto.Request(99L, "제목", "내용", true)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AgreementErrorCode.AGREEMENT_TYPE_NOT_FOUNT);
        verifyNoInteractions(agreementRepository, agreementLogRepository);
    }

    @Test
    @DisplayName("임시 약관은 기존 약관의 유형을 유지하고 저장된 약관의 비현행 이력을 만든다")
    void modifyTemporaryAgreementUsesExistingTypeAndSavedAgreement() {
        AgreementType type = agreementType(1L);
        Agreement original = Agreement.builder().id(10L).type(type).build();
        Agreement saved = Agreement.builder().id(20L).build();
        given(agreementRepository.findById(10L)).willReturn(Optional.of(original));
        given(agreementRepository.save(any(Agreement.class))).willReturn(saved);

        Agreement result = agreementService.modifyTemporaryAgreement(
                new BoTermsDto.Request(999L, "임시 약관", "임시 내용", false), 10L);

        ArgumentCaptor<Agreement> agreementCaptor = ArgumentCaptor.forClass(Agreement.class);
        verify(agreementRepository).save(agreementCaptor.capture());
        assertThat(agreementCaptor.getValue()).satisfies(agreement -> {
            assertThat(agreement.getType()).isSameAs(type);
            assertThat(agreement.getTitle()).isEqualTo("임시 약관");
            assertThat(agreement.getContents()).isEqualTo("임시 내용");
            assertThat(agreement.getIsRequired()).isFalse();
            assertThat(agreement.getIsDeleted()).isFalse();
        });
        ArgumentCaptor<AgreementLog> logCaptor = ArgumentCaptor.forClass(AgreementLog.class);
        verify(agreementLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getAgreement()).isSameAs(saved);
        assertThat(logCaptor.getValue().getIsCurrent()).isFalse();
        assertThat(result).isSameAs(saved);
        verifyNoInteractions(agreementTypeRepository);
    }

    @Test
    @DisplayName("약관 ID와 이력 ID가 달라도 선택한 약관만 현행으로 수정한다")
    void modifyAgreementSelectsLogByAgreementInsteadOfLogId() {
        AgreementType type = agreementType(1L);
        Agreement previous = Agreement.builder().id(10L).type(type).build();
        Agreement target = Agreement.builder().id(20L).type(type)
                .title("이전 제목").contents("이전 내용").isRequired(false).build();
        AgreementLog previousLog = AgreementLog.builder().id(20L).agreement(previous).isCurrent(true).build();
        AgreementLog targetLog = AgreementLog.builder().id(200L).agreement(target).isCurrent(false).build();
        List<Agreement> sameType = List.of(previous, target);
        given(agreementRepository.findById(20L)).willReturn(Optional.of(target));
        given(agreementRepository.findByType(type)).willReturn(sameType);
        given(agreementLogRepository.findByAgreementIn(sameType)).willReturn(List.of(previousLog, targetLog));

        Agreement result = agreementService.modifyAgreement(
                new BoTermsDto.Request(999L, "새로운 제목", "새로운 내용", true), 20L);

        assertThat(result).isSameAs(target);
        assertThat(target.getTitle()).isEqualTo("새로운 제목");
        assertThat(target.getContents()).isEqualTo("새로운 내용");
        assertThat(target.getIsRequired()).isTrue();
        assertThat(target.getType()).isSameAs(type);
        assertThat(previousLog.getIsCurrent()).isFalse();
        assertThat(targetLog.getIsCurrent()).isTrue();
        verify(agreementLogRepository, never()).findById(any());
    }

    @Test
    @DisplayName("수정할 약관이 없으면 조회 실패 예외를 던진다")
    void modifyAgreementRejectsUnknownAgreement() {
        given(agreementRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> agreementService.modifyAgreement(
                new BoTermsDto.Request(1L, "제목", "내용", true), 99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AgreementErrorCode.AGREEMENT_NOT_FOUNT);
        verifyNoInteractions(agreementLogRepository);
    }

    @Test
    @DisplayName("수정할 약관의 이력이 없으면 내용을 변경하지 않고 예외를 던진다")
    void modifyAgreementRejectsMissingLogBeforeMutation() {
        AgreementType type = agreementType(1L);
        Agreement target = Agreement.builder().id(20L).type(type)
                .title("기존 제목").contents("기존 내용").isRequired(false).build();
        Agreement previous = Agreement.builder().id(10L).type(type).build();
        AgreementLog previousLog = AgreementLog.builder().id(100L).agreement(previous).isCurrent(true).build();
        List<Agreement> sameType = List.of(previous, target);
        given(agreementRepository.findById(20L)).willReturn(Optional.of(target));
        given(agreementRepository.findByType(type)).willReturn(sameType);
        given(agreementLogRepository.findByAgreementIn(sameType)).willReturn(List.of(previousLog));

        assertThatThrownBy(() -> agreementService.modifyAgreement(
                new BoTermsDto.Request(1L, "수정 제목", "수정 내용", true), 20L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AgreementErrorCode.AGREEMENT_LOG_NOT_FOUNT);
        assertThat(target.getTitle()).isEqualTo("기존 제목");
        assertThat(target.getContents()).isEqualTo("기존 내용");
        assertThat(target.getIsRequired()).isFalse();
        assertThat(previousLog.getIsCurrent()).isTrue();
    }

    @Test
    @DisplayName("백오피스 약관 목록을 약관 ID와 현행 여부가 담긴 응답으로 변환한다")
    void findAgreementListMapsAgreementAndCurrentState() {
        Pageable pageable = PageRequest.of(0, 10);
        Agreement agreement = Agreement.builder().id(100L).title("약관").build();
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
        ReflectionTestUtils.setField(agreement, "createdAt", createdAt);
        AgreementLog log = AgreementLog.builder().id(500L).agreement(agreement).isCurrent(false).build();
        given(agreementLogRepository.findByAgreemnetIsCurrent(true, pageable, 1L)).willReturn(List.of(log));

        List<BoTermsListDto.Response.TermsItem> result = agreementService.findAgreementList(pageable, 1L);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.getId()).isEqualTo(100L);
            assertThat(item.getTitle()).isEqualTo("약관");
            assertThat(item.getCreateAt()).isEqualTo(createdAt);
            assertThat(item.isCurrent()).isFalse();
        });
    }

    @Test
    @DisplayName("백오피스 상세 조회는 목록이 제공한 약관 ID로 상세 응답을 만든다")
    void findBoAgreementUsesAgreementIdAndMapsDetails() {
        Agreement agreement = Agreement.builder().id(100L).title("상세 약관").contents("상세 내용").build();
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
        ReflectionTestUtils.setField(agreement, "createdAt", createdAt);
        given(agreementRepository.findById(100L)).willReturn(Optional.of(agreement));

        BoTermsDetailDto.Response result = agreementService.findBoAgreement(100L);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getTitle()).isEqualTo("상세 약관");
        assertThat(result.getContents()).isEqualTo("상세 내용");
        assertThat(result.getCreateAt()).isEqualTo(createdAt);
        verifyNoInteractions(agreementLogRepository);
    }

    @Test
    @DisplayName("백오피스 상세 조회 대상 약관이 없으면 예외를 던진다")
    void findBoAgreementRejectsUnknownAgreement() {
        given(agreementRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> agreementService.findBoAgreement(99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AgreementErrorCode.AGREEMENT_NOT_FOUNT);
    }

    @Test
    @DisplayName("사용자 상세 조회는 목록이 제공한 약관 ID로 내용 응답을 만든다")
    void findUserAgreementUsesAgreementIdAndMapsContents() {
        Agreement agreement = Agreement.builder().id(100L).title("약관 제목").contents("약관 내용").build();
        given(agreementRepository.findById(100L)).willReturn(Optional.of(agreement));

        TermsDetailDto.Response result = agreementService.findUserAgreement(100L);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getContents()).isEqualTo("약관 내용");
        verifyNoInteractions(agreementLogRepository);
    }

    @Test
    @DisplayName("사용자 상세 조회 대상 약관이 없으면 예외를 던진다")
    void findUserAgreementRejectsUnknownAgreement() {
        given(agreementRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> agreementService.findUserAgreement(99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AgreementErrorCode.AGREEMENT_NOT_FOUNT);
    }

    @Test
    @DisplayName("약관 삭제는 해당 약관을 삭제 상태로 변경한다")
    void deleteAgreementMarksDeleted() {
        Agreement agreement = Agreement.builder().id(10L).build();
        given(agreementRepository.findById(10L)).willReturn(Optional.of(agreement));

        assertThat(agreementService.deleteAgreement(10L)).isSameAs(agreement);

        assertThat(agreement.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("삭제할 약관이 없으면 조회 실패 예외를 던진다")
    void deleteAgreementRejectsUnknownAgreement() {
        given(agreementRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> agreementService.deleteAgreement(99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AgreementErrorCode.AGREEMENT_NOT_FOUNT);
    }

    @Test
    @DisplayName("약관의 유형을 조회한다")
    void findAgreementTypeReturnsAgreementType() {
        AgreementType type = agreementType(1L);
        given(agreementRepository.findById(10L))
                .willReturn(Optional.of(Agreement.builder().id(10L).type(type).build()));

        assertThat(agreementService.findAgreementType(10L)).isSameAs(type);
    }

    @Test
    @DisplayName("약관이 없으면 약관 유형을 조회할 수 없다")
    void findAgreementTypeRejectsUnknownAgreement() {
        given(agreementRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> agreementService.findAgreementType(99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AgreementErrorCode.AGREEMENT_NOT_FOUNT);
    }

    @Test
    @DisplayName("약관 유형 ID로 유형을 조회한다")
    void findAgreementTypeByIdReturnsType() {
        AgreementType type = agreementType(1L);
        given(agreementTypeRepository.findById(1L)).willReturn(Optional.of(type));

        assertThat(agreementService.findAgreementTypeById(1L)).isSameAs(type);
    }

    @Test
    @DisplayName("없는 약관 유형 ID를 조회하면 예외를 던진다")
    void findAgreementTypeByIdRejectsUnknownType() {
        given(agreementTypeRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> agreementService.findAgreementTypeById(99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AgreementErrorCode.AGREEMENT_TYPE_NOT_FOUNT);
    }

    @Test
    @DisplayName("약관 유형 목록을 유형 응답으로 변환한다")
    void findTypeTermsListMapsTypes() {
        AgreementType first = agreementType(1L);
        AgreementType second = agreementType(2L);
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
        ReflectionTestUtils.setField(first, "createdAt", createdAt);
        given(agreementTypeRepository.findAll()).willReturn(List.of(first, second));

        List<BoTypeTermsListDto.Response.TermsTypeItem> result = agreementService.findTypeTermsList();

        assertThat(result).extracting(BoTypeTermsListDto.Response.TermsTypeItem::getId)
                .containsExactly(1L, 2L);
        assertThat(result.getFirst().getTitle()).isEqualTo("유형 1");
        assertThat(result.getFirst().getCreateAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("약관 유형 저장 요청으로 삭제되지 않은 새 유형을 생성한다")
    void saveTermTypeMapsRequest() {
        given(agreementTypeRepository.save(any(AgreementType.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        AgreementType result = agreementService.saveTermType(new BoTypeTermsDto.Request("새 유형"));

        assertThat(result.getName()).isEqualTo("새 유형");
        assertThat(result.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("약관 유형 수정 요청으로 유형명을 변경한다")
    void modifyAgreementTypeChangesName() {
        AgreementType type = agreementType(1L);
        given(agreementTypeRepository.findById(1L)).willReturn(Optional.of(type));

        agreementService.modifyAgreementType(1L, new BoTypeTermsDto.Request("수정 유형"));

        assertThat(type.getName()).isEqualTo("수정 유형");
    }

    @Test
    @DisplayName("약관 유형을 삭제 상태로 변경한다")
    void deleteTermTypeMarksDeleted() {
        AgreementType type = agreementType(1L);
        given(agreementTypeRepository.findById(1L)).willReturn(Optional.of(type));

        assertThat(agreementService.deleteTermType(1L)).isSameAs(type);

        assertThat(type.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("삭제할 약관 유형이 없으면 조회 실패 예외를 던진다")
    void deleteTermTypeRejectsUnknownType() {
        given(agreementTypeRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> agreementService.deleteTermType(99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AgreementErrorCode.AGREEMENT_TYPE_NOT_FOUNT);
    }

    @Test
    @DisplayName("사용자 약관 목록을 ID와 제목이 담긴 응답으로 변환한다")
    void findUserTermsMapsCurrentAgreements() {
        given(agreementRepository.findByAgreementsIsCurrentAndIsDeleted())
                .willReturn(List.of(Agreement.builder().id(10L).title("이용 약관").build()));

        List<TermsListDto.Response.TermsItem> result = agreementService.findByAgreementsIsCurrentAndIsDeleted();

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.getId()).isEqualTo(10L);
            assertThat(item.getTitle()).isEqualTo("이용 약관");
        });
    }

    private AgreementType agreementType(Long id) {
        return AgreementType.builder().id(id).name("유형 " + id).isDeleted(false).build();
    }
}
