package org.example.knockin.service.impl;
 
import org.example.knockin.board.dto.BoardDetailDto.Response.Lifestyle;
import org.example.knockin.life.dto.ModifyProfileLifeStyleDto;
import org.example.knockin.life.entity.LifePattern;
import org.example.knockin.life.entity.LifePatternInformation;
import org.example.knockin.life.service.impl.LifeStyleServiceImpl;
import org.example.knockin.life.service.impl.MemberLifePatternService;
import org.example.knockin.life.entity.LifePatternType;
import org.example.knockin.life.entity.MemberLifePattern;
import org.example.knockin.life.entity.MemberLifePatternLog;
import org.example.knockin.life.entity.MemberLifePatternLogDegree;
import org.example.knockin.life.repository.MemberLifePatternLogDegreeRepository;
import org.example.knockin.life.repository.MemberLifePatternLogRepository;
import org.example.knockin.life.repository.MemberLifePatternRepository;
import org.example.knockin.life.repository.row.MatchingLifestyleRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
 
import java.util.List;
import java.util.Optional;
 
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private MemberLifePatternLogDegreeRepository memberLifePatternLogDegreeRepository;

    @Mock
    private LifeStyleServiceImpl lifeStyleService;
 
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
    @DisplayName("회원 생활 패턴 수정 시 같은 유형의 새 옵션으로 변경한다")
    void modifyLifeStyleUpdatesSelectedPattern() {
        Long memberId = 1L;
        Long memberLifePatternId = 2L;
        LifePattern pattern = LifePattern.builder().id(100L).build();
        LifePatternInformation oldOption = LifePatternInformation.builder().id(10L).lifePattern(pattern).build();
        LifePatternInformation newOption = LifePatternInformation.builder().id(11L).lifePattern(pattern).build();
        MemberLifePattern memberPattern = MemberLifePattern.builder()
                .id(memberLifePatternId)
                .memberId(memberId)
                .lifePatternInformation(oldOption)
                .build();
        ModifyProfileLifeStyleDto.Request.LifeStyleInfo change = new ModifyProfileLifeStyleDto.Request.LifeStyleInfo();
        change.setId(memberLifePatternId);
        change.setLifestyleId(newOption.getId());
        ModifyProfileLifeStyleDto.Request request = ModifyProfileLifeStyleDto.Request.builder()
                .lifestyles(List.of(change))
                .build();
        given(lifeStyleService.findLifePatternInformationAllById(List.of(newOption.getId())))
                .willReturn(List.of(newOption));
        given(memberLifePatternRepository.findByMemberId(memberId)).willReturn(List.of(memberPattern));

        memberLifePatternService.modifyLifeStyle(request, memberId);

        assertThat(memberPattern.getLifePatternInformation()).isSameAs(newOption);
    }

    @Test
    @DisplayName("회원 생활 패턴과 새 차수의 이력을 함께 저장한다")
    void saveMemberLifePatternsSavesPatternsAndLogsWithNextDegree() {
        Long memberId = 1L;
        LifePatternInformation sleepOption = LifePatternInformation.builder().id(10L).build();
        LifePatternInformation smokingOption = LifePatternInformation.builder().id(11L).build();
        List<Long> optionIds = List.of(sleepOption.getId(), smokingOption.getId());
        given(lifeStyleService.findLifePatternInformationByIds(optionIds))
                .willReturn(List.of(sleepOption, smokingOption));
        given(memberLifePatternLogDegreeRepository.findMaxmemberLifePatternLogDegree(memberId))
                .willReturn(Optional.of(2L));
        given(memberLifePatternLogDegreeRepository.save(any(MemberLifePatternLogDegree.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(memberLifePatternRepository.saveAll(any())).willReturn(List.of(
                MemberLifePattern.builder().id(20L).build(),
                MemberLifePattern.builder().id(21L).build()
        ));

        List<Long> savedIds = memberLifePatternService.saveMemberLifePatterns(memberId, optionIds);

        assertThat(savedIds).containsExactly(20L, 21L);
        ArgumentCaptor<Iterable<MemberLifePattern>> patternsCaptor = ArgumentCaptor.captor();
        verify(memberLifePatternRepository).saveAll(patternsCaptor.capture());
        assertThat(patternsCaptor.getValue())
                .extracting(MemberLifePattern::getMemberId, MemberLifePattern::getLifePatternInformation)
                .containsExactly(tuple(memberId, sleepOption), tuple(memberId, smokingOption));

        ArgumentCaptor<MemberLifePatternLogDegree> degreeCaptor = ArgumentCaptor.forClass(MemberLifePatternLogDegree.class);
        verify(memberLifePatternLogDegreeRepository).save(degreeCaptor.capture());
        MemberLifePatternLogDegree degree = degreeCaptor.getValue();
        assertThat(degree.getDegree()).isEqualTo(3L);

        ArgumentCaptor<Iterable<MemberLifePatternLog>> logsCaptor = ArgumentCaptor.captor();
        verify(memberLifePatternLogRepository).saveAll(logsCaptor.capture());
        assertThat(logsCaptor.getValue())
                .extracting(MemberLifePatternLog::getMemberId, MemberLifePatternLog::getLifePatternInformation,
                        MemberLifePatternLog::getMemberLifePatternLogDegree)
                .containsExactly(tuple(memberId, sleepOption, degree), tuple(memberId, smokingOption, degree));
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
