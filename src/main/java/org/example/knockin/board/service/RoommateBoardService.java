package org.example.knockin.board.service;

import java.io.IOException;
import java.util.List;

import org.example.knockin.board.dto.*;
import org.example.knockin.declaration.dto.ReportDto;
import org.example.knockin.board.dto.BoardListDto.Response;
import org.example.knockin.board.entity.RoommateBoard;
import org.jspecify.annotations.Nullable;
import org.example.knockin.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

public interface RoommateBoardService {
    BoardDto.Response save(BoardDto.Request request, Long memberId, @Nullable List<MultipartFile> files);
    BoardPreSignedDto.Response saveBoardPreSigned(BoardPreSignedDto.Request request, Long memberId);
    Slice<Response> getBoardList(BoardListDto.Request request, Pageable pageable, @Nullable Long requesterId);
    BoardDetailDto.Response getBoardDetail(Long boardId, Long memberId);
    Page<MyBoardListDto.Response.BoardItem> getMyBoardList(Pageable page, Member member);
    BoardEditDto.Response getEditForm(Long memberId, Long boardId);

    @Transactional(rollbackFor = IOException.class)
    BoardModifyDto.Response modify(Long memberId, Long boardId, BoardModifyDto.Request request, @Nullable List<MultipartFile> files);

    BoardPreSignedModifyDto.Response modifyBoardPreSigned(Long memberId, Long boardId, BoardPreSignedModifyDto.Request request);

    BoardDto.Response likeBoard(Long boardId, Long memberId);

    BoardDto.Response deleteBoard(Long boardId, Long memberId);

    ReportDto.Response reportBoard(ReportDto.Request request, Long boardId, Long memberId);

    List<BoBoardListDto.Response.BoardInfo> findBackOfficeBoardList(Pageable pageable, BoBoardListDto.Request request);

    BoBoardDetailDto.Response findBackOffcieBoard(Long id);

    RoommateBoard deleteBackOfficeBoard(Long id, String rejectReason);
    RoommateBoard recoverDeleteBoard(Long id);
}
