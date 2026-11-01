package org.example.knockin.service.impl;
 
import org.example.knockin.agreement.service.impl.AgreementServiceImpl;
import org.example.knockin.agreement.dto.BoTermsListDto;
import org.example.knockin.agreement.dto.BoTypeTermsListDto;
import java.time.LocalDateTime;
import org.example.knockin.agreement.entity.Agreement;
import org.example.knockin.agreement.entity.AgreementLog;
import org.example.knockin.agreement.entity.AgreementType;
import org.example.knockin.global.exception.AgreementErrorCode;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.agreement.repository.AgreementLogRepository;
import org.example.knockin.agreement.repository.AgreementRepository;
import org.example.knockin.agreement.repository.AgreementTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
 
import java.util.List;
import java.util.Optional;
 
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
 
@ExtendWith(MockitoExtension.class)
@DisplayName("약관동의 관리 서비스 테스트")
class AgreementServiceImplTest {
 
    @Mock
    private AgreementRepository agreementRepository;
 
    @Mock
    private AgreementLogRepository agreementLogRepository;
 
    @Mock
    private AgreementTypeRepository agreementTypeRepository;
 
    @InjectMocks
    private AgreementServiceImpl agreementService;
 
    @Test
    @DisplayName("약관 등록 성공 테스트")
    void saveAgreementSuccessTest() {
        // given
        AgreementType agreementType = AgreementType.builder().id(1L).name("개인정보처리방침").isDeleted(false).build();
        Agreement agreement = Agreement.builder()
                .title("약관 제목")
                .contents("약관 내용")
                .isRequired(true)
                .type(agreementType)
                .build();
 
        given(agreementRepository.save(agreement)).willReturn(agreement);
 
        // when
        Agreement result = agreementService.saveAgreement(agreement);
 
        // then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("약관 제목");
        verify(agreementLogRepository).save(any(AgreementLog.class));
        verify(agreementRepository).save(agreement);
    }
 
    @Test
    @DisplayName("임시 약관 등록 성공 테스트")
    void modifyTemporaryAgreementSuccessTest() {
        // given
        AgreementType agreementType = AgreementType.builder().id(1L).name("개인정보처리방침").isDeleted(false).build();
        Agreement agreement = Agreement.builder()
                .title("임시 약관")
                .contents("임시 내용")
                .type(agreementType)
                .build();
        given(agreementRepository.save(agreement)).willReturn(agreement);
 
        // when
        Agreement result = agreementService.modifyTemporaryAgreement(agreement);
 
        // then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("임시 약관");
        verify(agreementLogRepository).save(any(AgreementLog.class));
        verify(agreementRepository).save(agreement);
    }
 
    @Test
    @DisplayName("기존 약관 수정 성공 테스트")
    void modifyAgreementSuccessTest() {
        // given
        Long agreementId = 1L;
        AgreementType agreementType = AgreementType.builder().id(1L).name("개인정보처리방침").isDeleted(false).build();
        Agreement existingAgreement = spy(Agreement.builder()
                .id(agreementId)
                .title("이전 제목")
                .contents("이전 내용")
                .isRequired(false)
                .type(agreementType)
                .build());
        Agreement newAgreement = Agreement.builder()
                .title("새로운 제목")
                .contents("새로운 내용")
                .isRequired(true)
                .type(agreementType)
                .build();
 
        given(agreementRepository.findById(agreementId)).willReturn(Optional.of(existingAgreement));
        given(agreementRepository.findByType(agreementType)).willReturn(List.of(existingAgreement));
        given(agreementLogRepository.findByAgreementIn(any())).willReturn(List.of(mock(AgreementLog.class)));
        AgreementLog mockLog = mock(AgreementLog.class);
        given(agreementLogRepository.findById(agreementId)).willReturn(Optional.of(mockLog));
 
        // when
        Agreement result = agreementService.modifyAgreement(newAgreement, agreementId);
 
        // then
        assertThat(result).isNotNull();
        verify(existingAgreement).modifyAgreement(newAgreement);
        verify(agreementLogRepository).findByAgreementIn(any());
        verify(mockLog).enableCurrent();
    }
 
    @Test
    @DisplayName("약관 수정 시 대상을 찾지 못하면 BusinessException 발생")
    void modifyAgreementNotFoundTest() {
        // given
        Long agreementId = 1L;
        Agreement newAgreement = Agreement.builder().build();
        given(agreementRepository.findById(agreementId)).willReturn(Optional.empty());
 
        // when & then
        assertThatThrownBy(() -> agreementService.modifyAgreement(newAgreement, agreementId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AgreementErrorCode.AGREEMENT_NOT_FOUNT);
    }
 
    @Test
    @DisplayName("활성 약관 목록 조회 성공 테스트")
    void findAgreementListSuccessTest() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Agreement agreement = Agreement.builder().id(100L).title("약관").build();
        AgreementLog log = AgreementLog.builder().agreement(agreement).isCurrent(true).build();
 
        given(agreementLogRepository.findByAgreemnetIsCurrent(true, pageable, 1L)).willReturn(List.of(log));
 
        // when
        List<BoTermsListDto.Response.TermsItem> result = agreementService.findAgreementList(pageable, 1L);
 
        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(100L);
    }
 
    @Test
    @DisplayName("단건 약관 상세 조회 성공 테스트")
    void findAgreementSuccessTest() {
        // given
        Long id = 100L;
        Agreement agreement = Agreement.builder().id(id).title("상세 약관").build();
        AgreementLog log = AgreementLog.builder().id(id).agreement(agreement).isCurrent(true).build();
 
        given(agreementLogRepository.findById(id)).willReturn(Optional.of(log));
 
        // when
        Agreement result = agreementService.findBoAgreement(id);
 
        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
    }
 
    @Test
    @DisplayName("단건 약관 상세 조회 시 대상을 찾지 못하면 BusinessException 발생")
    void findAgreementNotFoundTest() {
        // given
        Long id = 100L;
        given(agreementLogRepository.findById(id)).willReturn(Optional.empty());
 
        // when & then
        assertThatThrownBy(() -> agreementService.findBoAgreement(id))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AgreementErrorCode.AGREEMENT_NOT_FOUNT);
    }
 
    @Test
    @DisplayName("약관 삭제 성공 테스트")
    void deleteAgreementSuccessTest() {
        // given
        Long id = 100L;
        Agreement agreement = spy(Agreement.builder().id(id).isDeleted(false).build());
 
        given(agreementRepository.findById(id)).willReturn(Optional.of(agreement));
 
        // when
        Agreement result = agreementService.deleteAgreement(id);
 
        // then
        assertThat(result).isNotNull();
        verify(agreement).deleteAgreement();
    }
 
    @Test
    @DisplayName("약관 유형 조회 성공 테스트")
    void findAgreementTypeSuccessTest() {
        // given
        Long id = 100L;
        AgreementType agreementType = AgreementType.builder().id(3L).name("약관 유형").build();
        Agreement agreement = Agreement.builder().id(id).type(agreementType).build();
        given(agreementRepository.findById(id)).willReturn(Optional.of(agreement));
 
        // when
        AgreementType result = agreementService.findAgreementType(id);
 
        // then
        assertThat(result).isEqualTo(agreementType);
    }
 
    @Test
    @DisplayName("약관 유형 조회 시 대상을 찾지 못하면 BusinessException 발생")
    void findAgreementTypeNotFoundTest() {
        // given
        Long id = 100L;
        given(agreementRepository.findById(id)).willReturn(Optional.empty());
 
        // when & then
        assertThatThrownBy(() -> agreementService.findAgreementType(id))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AgreementErrorCode.AGREEMENT_NOT_FOUNT);
    }
 
    @Test
    @DisplayName("약관 유형 ID로 조회 성공 테스트")
    void findAgreementTypeByIdSuccessTest() {
        // given
        Long id = 1L;
        AgreementType agreementType = AgreementType.builder().id(id).name("유형").build();
        given(agreementTypeRepository.findById(id)).willReturn(Optional.of(agreementType));
 
        // when
        AgreementType result = agreementService.findAgreementTypeById(id);
 
        // then
        assertThat(result).isEqualTo(agreementType);
    }
 
    @Test
    @DisplayName("약관 유형 ID로 조회 시 대상을 찾지 못하면 BusinessException 발생")
    void findAgreementTypeByIdNotFoundTest() {
        // given
        Long id = 1L;
        given(agreementTypeRepository.findById(id)).willReturn(Optional.empty());
 
        // when & then
        assertThatThrownBy(() -> agreementService.findAgreementTypeById(id))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AgreementErrorCode.AGREEMENT_TYPE_NOT_FOUNT);
    }

    @Test
    @DisplayName("약관 유형 목록 조회 성공 테스트")
    void findTypeTermsListSuccessTest() {
        // given
        AgreementType type1 = AgreementType.builder().id(1L).name("유형 1").build();
        org.springframework.test.util.ReflectionTestUtils.setField(type1, "createdAt", LocalDateTime.now());
        AgreementType type2 = AgreementType.builder().id(2L).name("유형 2").build();
        org.springframework.test.util.ReflectionTestUtils.setField(type2, "createdAt", LocalDateTime.now());

        given(agreementTypeRepository.findAll()).willReturn(List.of(type1, type2));

        // when
        List<BoTypeTermsListDto.Response.TermsTypeItem> result = agreementService.findTypeTermsList();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getTitle()).isEqualTo("유형 1");
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getTitle()).isEqualTo("유형 2");
    }

    @Test
    @DisplayName("약관 유형 저장 성공 테스트")
    void saveTermTypeSuccessTest() {
        // given
        AgreementType agreementType = AgreementType.builder().name("새 유형").isDeleted(false).build();
        given(agreementTypeRepository.save(agreementType)).willReturn(agreementType);

        // when
        AgreementType result = agreementService.saveTermType(agreementType);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("새 유형");
        verify(agreementTypeRepository).save(agreementType);
    }

    @Test
    @DisplayName("약관 유형 삭제 성공 테스트")
    void deleteTermTypeSuccessTest() {
        // given
        Long termTypeId = 1L;
        AgreementType agreementType = spy(AgreementType.builder().id(termTypeId).name("유형").isDeleted(false).build());
        given(agreementTypeRepository.findById(termTypeId)).willReturn(Optional.of(agreementType));

        // when
        AgreementType result = agreementService.deleteTermType(termTypeId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getIsDeleted()).isTrue();
        verify(agreementType).deleteAgreementType();
    }

    @Test
    @DisplayName("약관 유형 삭제 시 대상을 찾지 못하면 BusinessException 발생")
    void deleteTermTypeNotFoundTest() {
        // given
        Long termTypeId = 1L;
        given(agreementTypeRepository.findById(termTypeId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> agreementService.deleteTermType(termTypeId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AgreementErrorCode.AGREEMENT_TYPE_NOT_FOUNT);
    }
}
