package org.example.knockin.service.impl;
 
import org.example.knockin.board.dto.BoardDetailDto.Response.Lifestyle;
import org.example.knockin.life.service.impl.MemberLifePatternService;
import org.example.knockin.life.entity.LifePatternType;
import org.example.knockin.life.entity.MemberLifePattern;
import org.example.knockin.life.entity.MemberLifePatternLog;
import org.example.knockin.life.repository.MemberLifePatternLogRepository;
import org.example.knockin.life.repository.MemberLifePatternRepository;
import org.example.knockin.life.repository.row.MatchingLifestyleRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
 
import java.util.List;
 
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
 
@ExtendWith(MockitoExtension.class)
@DisplayName("회원 생활 패턴 서비스")
class MemberLifePatternServiceTest {
 
    @Mock
    private MemberLifePatternRepository memberLifePatternRepository;
 
    @Mock
    private MemberLifePatternLogRepository memberLifePatternLogRepository;
 
    @InjectMocks
    private MemberLifePatternService memberLifePatternService;
 
    @Test
    @DisplayName("회원 생활 패턴 로그 목록을 전달하면 Repository에 저장하고 저장 결과를 반환한다")
    void saveMemberLifePatternLogAllTest() {
        // Given
        MemberLifePatternLog log = mock(MemberLifePatternLog.class);
        List<MemberLifePatternLog> list = List.of(log);
        given(memberLifePatternLogRepository.saveAll(list)).willReturn(list);
 
        // When
        List<MemberLifePatternLog> result = memberLifePatternService.saveMemberLifePatternLogAll(list);
 
        // Then
        assertThat(result).hasSize(1);
        verify(memberLifePatternLogRepository).saveAll(list);
    }
 
    @Test
    @DisplayName("회원 생활 패턴 목록을 전달하면 Repository에 저장하고 저장 결과를 반환한다")
    void saveMemberLifePatternAllTest() {
        // Given
        MemberLifePattern pattern = mock(MemberLifePattern.class);
        List<MemberLifePattern> list = List.of(pattern);
        given(memberLifePatternRepository.saveAll(list)).willReturn(list);
 
        // When
        List<MemberLifePattern> result = memberLifePatternService.saveMemberLifePatternAll(list);
 
        // Then
        assertThat(result).hasSize(1);
        verify(memberLifePatternRepository).saveAll(list);
    }
 
    @Test
    @DisplayName("회원이 주어지면 해당 회원의 생활 패턴 목록을 조회한다")
    void findByMemberTest() {
        // Given
        Long memberId = 1L;
        MemberLifePattern pattern = mock(MemberLifePattern.class);
        given(memberLifePatternRepository.findByMemberId(memberId)).willReturn(List.of(pattern));
 
        // When
        List<MemberLifePattern> result = memberLifePatternService.findByMemberId(memberId);
 
        // Then
        assertThat(result).hasSize(1);
        verify(memberLifePatternRepository).findByMemberId(memberId);
    }

    @Test
    @DisplayName("회원 ID 목록으로 매칭용 생활 패턴 행을 조회한다")
    void findMatchingRowByMemberIdsInReturnsMatchingLifestyleRows() {
        // Given
        List<Long> memberIds = List.of(1L, 2L);
        MatchingLifestyleRow row = new MatchingLifestyleRow(
                1L,
                10L,
                100L,
                1000L,
                "취침",
                "23:00",
                "일찍 자요",
                LifePatternType.SCALE,
                "sleep.png"
        );
        given(memberLifePatternRepository.findAllLifestyleByMemberIdIn(memberIds)).willReturn(List.of(row));

        // When
        List<MatchingLifestyleRow> result = memberLifePatternService.findMatchingRowByMemberIdsIn(memberIds);

        // Then
        assertThat(result).containsExactly(row);
        verify(memberLifePatternRepository).findAllLifestyleByMemberIdIn(memberIds);
    }

    @Test
    @DisplayName("회원 ID로 수정 폼에 표시할 생활 패턴 DTO 목록을 조회한다")
    void findLifeStyleDtoByMemberIdReturnsLifestyleDtos() {
        // Given
        Long memberId = 1L;
        Lifestyle lifestyle = new Lifestyle(
                10L, "취침", "23:00", "일찍 자요", LifePatternType.SCALE, "sleep.png");
        given(memberLifePatternRepository.getLifeStyleDto(memberId)).willReturn(List.of(lifestyle));

        // When
        List<Lifestyle> result = memberLifePatternService.findLifeStyleDtoByMemberId(memberId);

        // Then
        assertThat(result).containsExactly(lifestyle);
        verify(memberLifePatternRepository).getLifeStyleDto(memberId);
    }
}
