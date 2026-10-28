package org.example.knockin.service.impl;
 
import org.example.knockin.board.dto.BoardDetailDto.Response.ConditionWeight;
import org.example.knockin.life.service.impl.PreferenceConditionServiceImpl;
import org.example.knockin.life.entity.PreferenceCondition;
import org.example.knockin.life.entity.PreferenceConditionLog;
import org.example.knockin.life.entity.PreferenceConditionWeight;
import org.example.knockin.life.entity.PreferenceConditionWeightLog;
import org.example.knockin.life.repository.PreferenceConditionLogRepository;
import org.example.knockin.life.repository.PreferenceConditionRepository;
import org.example.knockin.life.repository.PreferenceConditionWeightLogRepository;
import org.example.knockin.life.repository.PreferenceConditionWeightRepository;
import org.example.knockin.life.repository.row.MatchingPreferenceConditionWeightRow;
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
@DisplayName("선호 조건 서비스")
class PreferenceConditionServiceImplTest {
 
    @Mock private PreferenceConditionRepository preferenceConditionRepository;
    @Mock private PreferenceConditionLogRepository preferenceConditionLogRepository;
    @Mock private PreferenceConditionWeightRepository preferenceConditionWeightRepository;
    @Mock private PreferenceConditionWeightLogRepository preferenceConditionWeightLogRepository;
 
    @InjectMocks
    private PreferenceConditionServiceImpl preferenceConditionService;
 
    @Test
    @DisplayName("선호도 조건 목록을 전달하면 Repository에 저장하고 저장 결과를 반환한다")
    void preferenceConditionSaveAllTest() {
        // Given
        PreferenceCondition pc = mock(PreferenceCondition.class);
        List<PreferenceCondition> list = List.of(pc);
        given(preferenceConditionRepository.saveAll(list)).willReturn(list);
 
        // When
        List<PreferenceCondition> result = preferenceConditionService.preferenceConditionSaveAll(list);
 
        // Then
        assertThat(result).hasSize(1);
        verify(preferenceConditionRepository).saveAll(list);
    }
 
    @Test
    @DisplayName("선호도 조건 로그 목록을 전달하면 Repository에 저장하고 저장 결과를 반환한다")
    void preferenceConditionLogSaveAllTest() {
        // Given
        PreferenceConditionLog log = mock(PreferenceConditionLog.class);
        List<PreferenceConditionLog> list = List.of(log);
        given(preferenceConditionLogRepository.saveAll(list)).willReturn(list);
 
        // When
        List<PreferenceConditionLog> result = preferenceConditionService.preferenceConditionLogSaveAll(list);
 
        // Then
        assertThat(result).hasSize(1);
        verify(preferenceConditionLogRepository).saveAll(list);
    }
 
    @Test
    @DisplayName("선호도 가중치 목록을 전달하면 Repository에 저장하고 저장 결과를 반환한다")
    void preferenceConditionWeightSaveAllTest() {
        // Given
        PreferenceConditionWeight pcw = mock(PreferenceConditionWeight.class);
        List<PreferenceConditionWeight> list = List.of(pcw);
        given(preferenceConditionWeightRepository.saveAll(list)).willReturn(list);
 
        // When
        List<PreferenceConditionWeight> result = preferenceConditionService.preferenceConditionWeightSaveAll(list);
 
        // Then
        assertThat(result).hasSize(1);
        verify(preferenceConditionWeightRepository).saveAll(list);
    }
 
    @Test
    @DisplayName("선호도 가중치 로그 목록을 전달하면 Repository에 저장하고 저장 결과를 반환한다")
    void preferenceConditionWeightLogSaveAllTest() {
        // Given
        PreferenceConditionWeightLog log = mock(PreferenceConditionWeightLog.class);
        List<PreferenceConditionWeightLog> list = List.of(log);
        given(preferenceConditionWeightLogRepository.saveAll(list)).willReturn(list);
 
        // When
        List<PreferenceConditionWeightLog> result = preferenceConditionService.preferenceConditionWeightLogSaveAll(list);
 
        // Then
        assertThat(result).hasSize(1);
        verify(preferenceConditionWeightLogRepository).saveAll(list);
    }
 
    @Test
    @DisplayName("회원이 주어지면 해당 회원의 선호도 조건 목록을 조회한다")
    void findPreferenceConditionByMemberTest() {
        // Given
        Long memberId = 1L;
        PreferenceCondition pc = mock(PreferenceCondition.class);
        given(preferenceConditionRepository.findByMemberId(memberId)).willReturn(List.of(pc));
 
        // When
        List<PreferenceCondition> result = preferenceConditionService.findPreferenceConditionByMemberId(memberId);
 
        // Then
        assertThat(result).hasSize(1);
        verify(preferenceConditionRepository).findByMemberId(memberId);
    }
 
    @Test
    @DisplayName("회원이 주어지면 해당 회원의 선호도 가중치 목록을 조회한다")
    void findPreferenceConditionWeightByMemberTest() {
        // Given
        Long memberId = 1L;
        PreferenceConditionWeight pcw = mock(PreferenceConditionWeight.class);
        given(preferenceConditionWeightRepository.findByMemberId(memberId)).willReturn(List.of(pcw));
 
        // When
        List<PreferenceConditionWeight> result = preferenceConditionService.findPreferenceConditionWeightByMemberId(memberId);
 
        // Then
        assertThat(result).hasSize(1);
        verify(preferenceConditionWeightRepository).findByMemberId(memberId);
    }
 
    @Test
    @DisplayName("회원 ID 목록으로 선호도 가중치 매칭 행을 조회한다")
    void findWeightRowByMemberIdsInReturnsMatchingWeightRows() {
        // Given
        List<Long> memberIds = List.of(1L, 2L);
        MatchingPreferenceConditionWeightRow row =
                new MatchingPreferenceConditionWeightRow(1L, 10L, 100L, "청결", "cleaning.png");
        given(preferenceConditionWeightRepository.findAllPreferenceConditionWeightByMemberIdIn(memberIds))
                .willReturn(List.of(row));

        // When
        List<MatchingPreferenceConditionWeightRow> result =
                preferenceConditionService.findWeightRowByMemberIdsIn(memberIds);

        // Then
        assertThat(result).containsExactly(row);
        verify(preferenceConditionWeightRepository).findAllPreferenceConditionWeightByMemberIdIn(memberIds);
    }

    @Test
    @DisplayName("회원 ID로 선호도 가중치 DTO 목록을 조회한다")
    void findAllConditionWeightByMemberIdReturnsConditionWeightDtos() {
        // Given
        Long memberId = 1L;
        ConditionWeight conditionWeight = new ConditionWeight(10L, "청결", "cleaning.png");
        given(preferenceConditionWeightRepository.getConditionWeightDtoByMemberId(memberId))
                .willReturn(List.of(conditionWeight));

        // When
        List<ConditionWeight> result = preferenceConditionService.findAllConditionWeightByMemberId(memberId);

        // Then
        assertThat(result).containsExactly(conditionWeight);
        verify(preferenceConditionWeightRepository).getConditionWeightDtoByMemberId(memberId);
    }

    @Test
    @DisplayName("회원이 주어지면 해당 회원의 선호도 가중치를 삭제하고 flush 한다")
    void deletePreferenceConditionWeightByMemberTest() {
        // Given
        Long memberId = 1L;
 
        // When
        preferenceConditionService.deletePreferenceConditionWeightByMemberId(memberId);
 
        // Then
        verify(preferenceConditionWeightRepository).deleteByMemberId(memberId);
        verify(preferenceConditionWeightRepository).flush();
    }
}
