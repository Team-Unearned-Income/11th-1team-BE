package org.example.knockin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.example.knockin.agreement.entity.Agreement;
import org.example.knockin.agreement.entity.AgreementLog;
import org.example.knockin.agreement.entity.MemberAgreement;
import org.example.knockin.agreement.repository.AgreementLogRepository;
import org.example.knockin.agreement.repository.AgreementRepository;
import org.example.knockin.agreement.repository.AgreementTypeRepository;
import org.example.knockin.agreement.repository.MemberAgreementRepository;
import org.example.knockin.agreement.service.impl.AgreementServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("약관 서비스의 회원 동의 관리")
class MemberAgreementTest {

    private static final Long MEMBER_ID = 7L;

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
    @DisplayName("최초 저장은 요청 약관의 현행 이력과 회원 ID를 동의 상태로 저장한다")
    void saveAllMemberAgreementCreatesConsentsForResolvedCurrentLogs() {
        List<Long> terms = List.of(10L, 20L);
        AgreementLog firstLog = agreementLog(100L, 10L);
        AgreementLog secondLog = agreementLog(200L, 20L);
        given(memberAgreementRepository.findByMemberId(MEMBER_ID)).willReturn(List.of());
        given(agreementLogRepository.findByAgreementLogIsCurrent(terms))
                .willReturn(List.of(firstLog, secondLog));
        given(memberAgreementRepository.saveAll(any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        List<MemberAgreement> result = agreementService.saveAllMemberAgreement(terms, MEMBER_ID);

        assertThat(result).hasSize(2).allSatisfy(agreement -> {
            assertThat(agreement.getMemberId()).isEqualTo(MEMBER_ID);
            assertThat(agreement.getIsAgreed()).isTrue();
        });
        assertThat(result).extracting(MemberAgreement::getAgreementLog)
                .containsExactly(firstLog, secondLog);
        verify(memberAgreementRepository, never()).save(any());
    }

    @Test
    @DisplayName("동의가 저장되면 온보딩 실패 여부로 false를 반환한다")
    void saveMemberAgreementReturnsFalseWhenConsentSaved() {
        List<Long> terms = List.of(10L);
        AgreementLog log = agreementLog(100L, 10L);
        given(memberAgreementRepository.findByMemberId(MEMBER_ID)).willReturn(List.of());
        given(agreementLogRepository.findByAgreementLogIsCurrent(terms)).willReturn(List.of(log));
        given(memberAgreementRepository.saveAll(any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        assertThat(agreementService.saveMemberAgreement(terms, MEMBER_ID)).isFalse();
    }

    @Test
    @DisplayName("요청 약관에 대응하는 현행 이력이 없으면 온보딩 실패 여부로 true를 반환한다")
    void saveMemberAgreementReturnsTrueWhenNoCurrentLogResolved() {
        List<Long> terms = List.of(99L);
        given(memberAgreementRepository.findByMemberId(MEMBER_ID)).willReturn(List.of());
        given(agreementLogRepository.findByAgreementLogIsCurrent(terms)).willReturn(List.of());
        given(memberAgreementRepository.saveAll(any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        assertThat(agreementService.saveMemberAgreement(terms, MEMBER_ID)).isTrue();
    }

    @Test
    @DisplayName("기존 내역이 있어도 요청한 현행 약관이 없으면 기존 동의를 철회하고 실패를 반환한다")
    void saveMemberAgreementDoesNotTreatRevokedHistoryAsSuccessfulConsent() {
        List<Long> invalidTerms = List.of(99L);
        MemberAgreement existing = memberAgreement(1L, agreementLog(100L, 10L), true);
        given(memberAgreementRepository.findByMemberId(MEMBER_ID)).willReturn(List.of(existing));
        given(agreementLogRepository.findByAgreementLogIsCurrent(invalidTerms)).willReturn(List.of());

        assertThat(agreementService.saveMemberAgreement(invalidTerms, MEMBER_ID)).isTrue();

        assertThat(existing.getIsAgreed()).isFalse();
        verify(memberAgreementRepository, never()).save(any());
        verify(memberAgreementRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("기존 내역이 있어도 약관을 빈 목록으로 저장하면 온보딩 실패를 반환한다")
    void saveMemberAgreementReturnsFailureForEmptySelectionWithHistory() {
        MemberAgreement existing = memberAgreement(1L, agreementLog(100L, 10L), false);
        given(memberAgreementRepository.findByMemberId(MEMBER_ID)).willReturn(List.of(existing));
        given(agreementLogRepository.findByAgreementLogIsCurrent(List.of())).willReturn(List.of());

        assertThat(agreementService.saveMemberAgreement(List.of(), MEMBER_ID)).isTrue();

        assertThat(existing.getIsAgreed()).isFalse();
    }

    @Test
    @DisplayName("기존 내역을 다시 저장하면 철회했던 동의는 재활성화하고 새 약관만 추가한다")
    void saveAllMemberAgreementSynchronizesExistingAndReturnsNewConsent() {
        AgreementLog firstLog = agreementLog(100L, 10L);
        AgreementLog removedLog = agreementLog(200L, 20L);
        AgreementLog newLog = agreementLog(300L, 30L);
        MemberAgreement retained = memberAgreement(1L, firstLog, false);
        MemberAgreement removed = memberAgreement(2L, removedLog, true);
        List<Long> terms = List.of(10L, 30L);
        given(memberAgreementRepository.findByMemberId(MEMBER_ID)).willReturn(List.of(retained, removed));
        given(agreementLogRepository.findByAgreementLogIsCurrent(terms)).willReturn(List.of(firstLog, newLog));
        given(memberAgreementRepository.save(any(MemberAgreement.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        List<MemberAgreement> result = agreementService.saveAllMemberAgreement(terms, MEMBER_ID);

        assertThat(retained.getIsAgreed()).isTrue();
        assertThat(removed.getIsAgreed()).isFalse();
        assertThat(result).hasSize(3).contains(retained, removed);
        assertThat(result.getLast()).satisfies(added -> {
            assertThat(added.getMemberId()).isEqualTo(MEMBER_ID);
            assertThat(added.getAgreementLog()).isSameAs(newLog);
            assertThat(added.getIsAgreed()).isTrue();
        });
        verify(memberAgreementRepository).save(any(MemberAgreement.class));
        verify(memberAgreementRepository, never()).saveAll(any());
        verify(memberAgreementRepository).findByMemberId(MEMBER_ID);
    }

    @Test
    @DisplayName("같은 이력 ID의 다른 객체를 요청해도 철회된 기존 동의를 재사용한다")
    void modifyMemberAgreementReactivatesRevokedConsentByLogId() {
        AgreementLog originalLog = agreementLog(100L, 10L);
        AgreementLog requestedLog = agreementLog(100L, 10L);
        MemberAgreement existing = memberAgreement(1L, originalLog, false);
        List<Long> terms = List.of(10L);
        given(memberAgreementRepository.findByMemberId(MEMBER_ID)).willReturn(List.of(existing));
        given(agreementLogRepository.findByAgreementLogIsCurrent(terms)).willReturn(List.of(requestedLog));

        agreementService.modifyMemberAgreement(terms, MEMBER_ID);

        assertThat(existing.getIsAgreed()).isTrue();
        verify(memberAgreementRepository, never()).save(any());
        verify(memberAgreementRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("선택하지 않은 동의는 철회하고 계속 선택한 동의는 유지한다")
    void modifyMemberAgreementRetainsOnlySelectedConsent() {
        AgreementLog selectedLog = agreementLog(100L, 10L);
        MemberAgreement selected = memberAgreement(1L, selectedLog, true);
        MemberAgreement removed = memberAgreement(2L, agreementLog(200L, 20L), true);
        List<Long> terms = List.of(10L);
        given(memberAgreementRepository.findByMemberId(MEMBER_ID)).willReturn(List.of(selected, removed));
        given(agreementLogRepository.findByAgreementLogIsCurrent(terms)).willReturn(List.of(selectedLog));

        agreementService.modifyMemberAgreement(terms, MEMBER_ID);

        assertThat(selected.getIsAgreed()).isTrue();
        assertThat(removed.getIsAgreed()).isFalse();
        verify(memberAgreementRepository, never()).save(any());
    }

    @Test
    @DisplayName("요청이 빈 목록이면 모든 기존 동의를 철회한다")
    void modifyMemberAgreementRevokesAllForEmptySelection() {
        MemberAgreement first = memberAgreement(1L, agreementLog(100L, 10L), true);
        MemberAgreement second = memberAgreement(2L, agreementLog(200L, 20L), true);
        given(memberAgreementRepository.findByMemberId(MEMBER_ID)).willReturn(List.of(first, second));
        given(agreementLogRepository.findByAgreementLogIsCurrent(List.of())).willReturn(List.of());

        agreementService.modifyMemberAgreement(List.of(), MEMBER_ID);

        assertThat(first.getIsAgreed()).isFalse();
        assertThat(second.getIsAgreed()).isFalse();
        verify(memberAgreementRepository, never()).save(any());
    }

    @Test
    @DisplayName("현행 이력이 바뀌면 과거 이력 동의는 철회하고 새 이력 동의를 저장한다")
    void modifyMemberAgreementReplacesOldLogOfSameAgreement() {
        AgreementLog oldLog = agreementLog(100L, 10L);
        AgreementLog newLog = agreementLog(101L, 10L);
        MemberAgreement previous = memberAgreement(1L, oldLog, true);
        List<Long> terms = List.of(10L);
        given(memberAgreementRepository.findByMemberId(MEMBER_ID)).willReturn(List.of(previous));
        given(agreementLogRepository.findByAgreementLogIsCurrent(terms)).willReturn(List.of(newLog));
        given(memberAgreementRepository.save(any(MemberAgreement.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        agreementService.modifyMemberAgreement(terms, MEMBER_ID);

        assertThat(previous.getIsAgreed()).isFalse();
        ArgumentCaptor<MemberAgreement> captor = ArgumentCaptor.forClass(MemberAgreement.class);
        verify(memberAgreementRepository).save(captor.capture());
        assertThat(captor.getValue().getAgreementLog()).isSameAs(newLog);
        assertThat(captor.getValue().getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(captor.getValue().getIsAgreed()).isTrue();
    }

    @Test
    @DisplayName("기존 동의가 없어도 변경 요청으로 신규 동의를 저장한다")
    void modifyMemberAgreementCreatesConsentWhenHistoryMissing() {
        AgreementLog log = agreementLog(100L, 10L);
        List<Long> terms = List.of(10L);
        given(memberAgreementRepository.findByMemberId(MEMBER_ID)).willReturn(List.of());
        given(agreementLogRepository.findByAgreementLogIsCurrent(terms)).willReturn(List.of(log));
        given(memberAgreementRepository.save(any(MemberAgreement.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        agreementService.modifyMemberAgreement(terms, MEMBER_ID);

        ArgumentCaptor<MemberAgreement> captor = ArgumentCaptor.forClass(MemberAgreement.class);
        verify(memberAgreementRepository).save(captor.capture());
        assertThat(captor.getValue().getAgreementLog()).isSameAs(log);
        assertThat(captor.getValue().getIsAgreed()).isTrue();
    }

    @Test
    @DisplayName("동의를 철회한 후 재동의해도 기존 행을 재활성화하고 중복 저장하지 않는다")
    void modifyMemberAgreementSupportsReconsentAfterRevocation() {
        AgreementLog log = agreementLog(100L, 10L);
        MemberAgreement existing = memberAgreement(1L, log, true);
        List<Long> terms = List.of(10L);
        given(memberAgreementRepository.findByMemberId(MEMBER_ID)).willReturn(List.of(existing));
        given(agreementLogRepository.findByAgreementLogIsCurrent(List.of())).willReturn(List.of());
        given(agreementLogRepository.findByAgreementLogIsCurrent(terms)).willReturn(List.of(log));

        agreementService.modifyMemberAgreement(List.of(), MEMBER_ID);
        assertThat(existing.getIsAgreed()).isFalse();
        agreementService.modifyMemberAgreement(terms, MEMBER_ID);

        assertThat(existing.getIsAgreed()).isTrue();
        verify(memberAgreementRepository, times(2)).findByMemberId(MEMBER_ID);
        verify(memberAgreementRepository, never()).save(any());
        verify(memberAgreementRepository, never()).saveAll(any());
    }

    private AgreementLog agreementLog(Long logId, Long agreementId) {
        return AgreementLog.builder()
                .id(logId)
                .agreement(Agreement.builder().id(agreementId).build())
                .isCurrent(true)
                .build();
    }

    private MemberAgreement memberAgreement(Long id, AgreementLog log, boolean agreed) {
        return MemberAgreement.builder().id(id).memberId(MEMBER_ID)
                .agreementLog(log).isAgreed(agreed).build();
    }
}
