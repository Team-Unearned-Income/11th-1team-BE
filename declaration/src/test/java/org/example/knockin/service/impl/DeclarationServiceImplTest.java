package org.example.knockin.service.impl;

import org.example.knockin.declaration.service.impl.DeclarationServiceImpl;
import org.example.knockin.declaration.dto.BoReportDoneListDto;
import org.example.knockin.declaration.dto.BoReportWaitListDto;
import org.example.knockin.declaration.entity.RoommateBoardDeclaration;
import org.example.knockin.declaration.entity.MemberDeclaration;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.DeclarationErrorCode;
import org.example.knockin.global.exception.MemberErrorCode;
import org.example.knockin.global.exception.RoommateBoardErrorCode;
import org.example.knockin.declaration.entity.DeclarationType;
import org.example.knockin.global.util.ReportType;
import org.example.knockin.declaration.repository.RoommateBoardDeclarationRepository;
import org.example.knockin.declaration.repository.MemberDeclarationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("신고 처리 서비스 테스트")
class DeclarationServiceImplTest {

    @Mock
    private MemberDeclarationRepository memberDeclarationRepository;

    @Mock
    private RoommateBoardDeclarationRepository roommateBoardDeclarationRepository;

    @InjectMocks
    private DeclarationServiceImpl declarationService;

    @Test
    void memberReportsStoreReporterAndReportedIds() {
        declarationService.reportMember(10L, 20L, "신고 사유");

        ArgumentCaptor<MemberDeclaration> captor = ArgumentCaptor.forClass(MemberDeclaration.class);
        verify(memberDeclarationRepository).existsByReporterIdAndReportedId(10L, 20L);
        verify(memberDeclarationRepository).save(captor.capture());
        assertThat(captor.getValue().getReporterId()).isEqualTo(10L);
        assertThat(captor.getValue().getReportedId()).isEqualTo(20L);
        assertThat(captor.getValue().getReason()).isEqualTo("신고 사유");
        assertThat(captor.getValue().getDeclarationType()).isEqualTo(DeclarationType.PENDING);
    }

    @Test
    void duplicateMemberReportsAreRejectedByIds() {
        given(memberDeclarationRepository.existsByReporterIdAndReportedId(10L, 20L)).willReturn(true);

        assertThatThrownBy(() -> declarationService.reportMember(10L, 20L, "신고 사유"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.DECLARATION_DUPLICATE);

        verify(memberDeclarationRepository, never()).save(any());
    }

    @Test
    void boardReportsStoreReporterAndBoardIds() {
        declarationService.reportBoard(30L, 10L, "신고 사유");

        ArgumentCaptor<RoommateBoardDeclaration> captor = ArgumentCaptor.forClass(RoommateBoardDeclaration.class);
        verify(roommateBoardDeclarationRepository).findByRoommateBoardIdAndMemberId(30L, 10L);
        verify(roommateBoardDeclarationRepository).save(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo(10L);
        assertThat(captor.getValue().getRoommateBoardId()).isEqualTo(30L);
        assertThat(captor.getValue().getReason()).isEqualTo("신고 사유");
        assertThat(captor.getValue().getDeclarationType()).isEqualTo(DeclarationType.PENDING);
    }

    @Test
    void duplicateBoardReportsAreRejectedByIds() {
        given(roommateBoardDeclarationRepository.findByRoommateBoardIdAndMemberId(30L, 10L))
                .willReturn(Optional.of(RoommateBoardDeclaration.builder().id(1L).build()));

        assertThatThrownBy(() -> declarationService.reportBoard(30L, 10L, "신고 사유"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", RoommateBoardErrorCode.ROOMMATE_BOARD_DECLARATION_DUPLICATE);

        verify(roommateBoardDeclarationRepository, never()).save(any());
    }

    @Test
    @DisplayName("신고 대기 목록 조회 성공 테스트 (정렬 기준 검증 포함)")
    void findReportWaitListSuccessTest() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime now = LocalDateTime.now();

        BoReportWaitListDto.Response.ReportInfo memberReport = new BoReportWaitListDto.Response.ReportInfo();
        memberReport.setId(1L);
        memberReport.setCreatedAt(now.minusHours(1));

        BoReportWaitListDto.Response.ReportInfo boardReport = new BoReportWaitListDto.Response.ReportInfo();
        boardReport.setId(2L);
        boardReport.setCreatedAt(now);

        given(memberDeclarationRepository.findReportWaitList(pageable)).willReturn(List.of(memberReport));
        given(roommateBoardDeclarationRepository.findReportWaitList(pageable)).willReturn(List.of(boardReport));

        // when
        List<BoReportWaitListDto.Response.ReportInfo> result = declarationService.findReportWaitList(pageable);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(2L);
        assertThat(result.get(1).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("신고 완료 목록 조회 성공 테스트 (정렬 기준 검증 포함)")
    void findReportDoneListSuccessTest() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime now = LocalDateTime.now();

        BoReportDoneListDto.Response.ReportInfo memberReport = new BoReportDoneListDto.Response.ReportInfo();
        memberReport.setId(1L);
        memberReport.setCreatedAt(now.minusHours(1));

        BoReportDoneListDto.Response.ReportInfo boardReport = new BoReportDoneListDto.Response.ReportInfo();
        boardReport.setId(2L);
        boardReport.setCreatedAt(now);

        given(memberDeclarationRepository.findReportDoneList(pageable)).willReturn(List.of(memberReport));
        given(roommateBoardDeclarationRepository.findReportDoneList(pageable)).willReturn(List.of(boardReport));

        // when
        List<BoReportDoneListDto.Response.ReportInfo> result = declarationService.findReportDoneList(pageable);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(2L);
        assertThat(result.get(1).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("신고 숨김(HIDDEN) 처리 성공 테스트 - 게시글 신고")
    void reportHiddenBoardSuccessTest() {
        // given
        Long id = 1L;
        RoommateBoardDeclaration boardDeclaration = spy(RoommateBoardDeclaration.builder().id(id).build());
        given(roommateBoardDeclarationRepository.findById(id)).willReturn(Optional.of(boardDeclaration));

        // when
        declarationService.reportHidden(id, ReportType.BOARD, "사유");

        // then
        verify(boardDeclaration).changeDeclarationType(DeclarationType.HIDDEN);
    }

    @Test
    @DisplayName("신고 숨김(HIDDEN) 처리 성공 테스트 - 사용자 신고")
    void reportHiddenMemberSuccessTest() {
        // given
        Long id = 1L;
        MemberDeclaration memberDeclaration = spy(MemberDeclaration.builder().id(id).build());
        given(memberDeclarationRepository.findById(id)).willReturn(Optional.of(memberDeclaration));

        // when
        declarationService.reportHidden(id, ReportType.MEMBER, "사유");

        // then
        verify(memberDeclaration).changeDeclarationType(DeclarationType.HIDDEN);
    }

    @Test
    @DisplayName("신고 숨김 처리 시 대상 신고를 찾을 수 없으면 BusinessException 발생")
    void reportHiddenNotFoundTest() {
        // given
        Long id = 1L;
        given(roommateBoardDeclarationRepository.findById(id)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> declarationService.reportHidden(id, ReportType.BOARD, "사유"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", DeclarationErrorCode.DECLARATION_NOT_FOUND);
    }

    @Test
    @DisplayName("신고 무조치(NOACTION) 처리 성공 테스트 - 게시글 신고")
    void reportNoActionBoardSuccessTest() {
        // given
        Long id = 1L;
        RoommateBoardDeclaration boardDeclaration = spy(RoommateBoardDeclaration.builder().id(id).build());
        given(roommateBoardDeclarationRepository.findById(id)).willReturn(Optional.of(boardDeclaration));

        // when
        declarationService.reportNoAction(id, ReportType.BOARD);

        // then
        verify(boardDeclaration).changeDeclarationType(DeclarationType.NOACTION);
    }

    @Test
    @DisplayName("신고 무조치(NOACTION) 처리 성공 테스트 - 사용자 신고")
    void reportNoActionMemberSuccessTest() {
        // given
        Long id = 1L;
        MemberDeclaration memberDeclaration = spy(MemberDeclaration.builder().id(id).build());
        given(memberDeclarationRepository.findById(id)).willReturn(Optional.of(memberDeclaration));

        // when
        declarationService.reportNoAction(id, ReportType.MEMBER);

        // then
        verify(memberDeclaration).changeDeclarationType(DeclarationType.NOACTION);
    }

    @Test
    @DisplayName("신고 정지(SUSPENDED) 처리 성공 테스트 - 게시글 신고")
    void reportSuspendedBoardSuccessTest() {
        // given
        Long id = 1L;
        RoommateBoardDeclaration boardDeclaration = spy(RoommateBoardDeclaration.builder().id(id).build());
        given(roommateBoardDeclarationRepository.findById(id)).willReturn(Optional.of(boardDeclaration));
 
        // when
        declarationService.reportSuspended(id, ReportType.BOARD, "사유");
 
        // then
        verify(boardDeclaration).changeDeclarationType(DeclarationType.SUSPENDED, "사유");
    }
 
    @Test
    @DisplayName("신고 정지(SUSPENDED) 처리 성공 테스트 - 사용자 신고")
    void reportSuspendedMemberSuccessTest() {
        // given
        Long id = 1L;
        MemberDeclaration memberDeclaration = MemberDeclaration.builder()
                .id(id)
                .reporterId(10L)
                .reportedId(20L)
                .declarationType(DeclarationType.PENDING)
                .build();
        given(memberDeclarationRepository.findById(id)).willReturn(Optional.of(memberDeclaration));

        // when
        declarationService.reportSuspended(id, ReportType.MEMBER, "사유");

        // then
        assertThat(memberDeclaration.getDeclarationType()).isEqualTo(DeclarationType.SUSPENDED);
        verifyNoInteractions(roommateBoardDeclarationRepository);
    }

    @Test
    @DisplayName("신고 ID로 신고 대상 회원 ID를 조회한다")
    void findReportedMemberIdUsesDeclarationId() {
        MemberDeclaration declaration = MemberDeclaration.builder()
                .id(100L).reporterId(10L).reportedId(20L).build();
        given(memberDeclarationRepository.findById(100L)).willReturn(Optional.of(declaration));

        assertThat(declarationService.findReportedMemberId(100L)).isEqualTo(20L);
    }

    @Test
    @DisplayName("신고 대상 회원 조회 시 신고가 없으면 예외를 던진다")
    void findReportedMemberIdRejectsMissingDeclaration() {
        given(memberDeclarationRepository.findById(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> declarationService.findReportedMemberId(100L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", DeclarationErrorCode.DECLARATION_NOT_FOUND);
    }
}
