package org.example.knockin.service.impl;
 
import org.example.knockin.agreement.entity.AgreementLog;
import org.example.knockin.agreement.entity.MemberAgreement;
import org.example.knockin.agreement.service.impl.MemberAgreementServiceImpl;
import org.example.knockin.member.entity.Member;
import org.example.knockin.agreement.repository.MemberAgreementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
 
import java.util.List;
 
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
 
@ExtendWith(MockitoExtension.class)
class MemberAgreementServiceImplTest {
 
    @Mock
    private MemberAgreementRepository memberAgreementRepository;
 
    @InjectMocks
    private MemberAgreementServiceImpl memberAgreementService;
 
    @Test
    @DisplayName("동의 내역 모두 저장 테스트")
    void saveAllTest() {
        MemberAgreement agreement = mock(MemberAgreement.class);
        List<MemberAgreement> list = List.of(agreement);
        given(memberAgreementRepository.saveAll(list)).willReturn(list);
 
        List<MemberAgreement> result = memberAgreementService.saveAll(list);
 
        assertThat(result).hasSize(1);
        verify(memberAgreementRepository).saveAll(list);
    }
 
    @Test
    @DisplayName("동의 내역 저장 테스트")
    void saveTest() {
        MemberAgreement agreement = mock(MemberAgreement.class);
        given(memberAgreementRepository.save(any(MemberAgreement.class))).willReturn(agreement);
 
        MemberAgreement result = memberAgreementService.save(agreement);
 
        assertThat(result).isEqualTo(agreement);
        verify(memberAgreementRepository).save(agreement);
    }
 
    @Test
    @DisplayName("회원별 동의 내역 조회 테스트")
    void findByMemberTest() {
        Member member = mock(Member.class);
        MemberAgreement agreement = mock(MemberAgreement.class);
        given(memberAgreementRepository.findByMemberId(member.getId())).willReturn(List.of(agreement));
 
        List<MemberAgreement> result = memberAgreementService.findByMemberId(member.getId());
 
        assertThat(result).hasSize(1);
        verify(memberAgreementRepository).findByMemberId(member.getId());
    }
 
    @Test
    @DisplayName("제외 리스트 외 회원별 동의 내역 조회 테스트")
    void findByMemberAndAgreementLogNotInTest() {
        Member member = mock(Member.class);
        AgreementLog log = mock(AgreementLog.class);
        List<AgreementLog> skipList = List.of(log);
        MemberAgreement agreement = mock(MemberAgreement.class);
        given(memberAgreementRepository.findByMemberAndAgreementLogNotIn(member.getId(), skipList)).willReturn(List.of(agreement));
 
        List<MemberAgreement> result = memberAgreementService.findByMemberIdAndAgreementLogNotIn(member.getId(), skipList);
 
        assertThat(result).hasSize(1);
        verify(memberAgreementRepository).findByMemberAndAgreementLogNotIn(member.getId(), skipList);
    }
}
