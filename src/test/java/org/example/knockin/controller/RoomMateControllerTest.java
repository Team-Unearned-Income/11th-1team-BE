package org.example.knockin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.example.knockin.board.controller.RoomMateController;
import org.example.knockin.board.dto.BoardListDto;
import org.example.knockin.global.auth.dto.PrincipalDetails;
import org.example.knockin.member.entity.Member;
import org.example.knockin.board.service.RoommateBoardService;
import org.example.knockin.mate.service.RoommateMatchingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("룸메이트 게시글 Controller")
class RoomMateControllerTest {

    @Mock
    private RoommateBoardService roommateBoardService;

    @Mock
    private RoommateMatchingService roommateMatchingService;

    @InjectMocks
    private RoomMateController roomMateController;

    @Test
    @DisplayName("비로그인 게시글 조회는 null 회원 ID를 서비스에 전달한다")
    void findBoardListPassesNullRequesterIdForAnonymousUser() {
        // Given
        BoardListDto.Request request = new BoardListDto.Request();
        request.setKeyword("원룸");
        Pageable pageable = PageRequest.of(0, 20);
        Slice<BoardListDto.Response> boards = new SliceImpl<>(List.of(), pageable, false);
        when(roommateBoardService.getBoardList(eq(request), eq(pageable), isNull()))
                .thenReturn(boards);

        // When
        var response = roomMateController.findBoardList(null, request, pageable);

        // Then
        assertThat(response.getStatusValue()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(boards);
        verify(roommateBoardService).getBoardList(request, pageable, null);
    }

    @Test
    @DisplayName("로그인 게시글 조회는 인증 회원 ID를 서비스에 전달한다")
    void findBoardListPassesAuthenticatedRequesterId() {
        // Given
        Long requesterId = 42L;
        PrincipalDetails details = org.mockito.Mockito.mock(PrincipalDetails.class);
        Member member = org.mockito.Mockito.mock(Member.class);
        BoardListDto.Request request = new BoardListDto.Request();
        Pageable pageable = PageRequest.of(1, 1);
        Slice<BoardListDto.Response> boards = new SliceImpl<>(List.of(new BoardListDto.Response()), pageable, true);
        when(details.getMember()).thenReturn(member);
        when(member.getId()).thenReturn(requesterId);
        when(roommateBoardService.getBoardList(request, pageable, requesterId))
                .thenReturn(boards);

        // When
        var response = roomMateController.findBoardList(details, request, pageable);

        // Then
        assertThat(response.getStatusValue()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(boards);
        verify(roommateBoardService).getBoardList(request, pageable, requesterId);
    }
}
