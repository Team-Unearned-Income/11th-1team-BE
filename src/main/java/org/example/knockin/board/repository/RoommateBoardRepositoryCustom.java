package org.example.knockin.board.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.example.knockin.board.repository.row.BasicInfoRow;
import org.example.knockin.board.repository.row.BoardBaseRow;
import org.example.knockin.board.repository.row.EditFormRow;
import org.example.knockin.board.repository.row.MyRoommateBoardRow;
import org.example.knockin.board.dto.BoBoardDetailDto;
import org.example.knockin.board.dto.BoBoardListDto;
import org.example.knockin.board.dto.BoardListDto;
import org.example.knockin.member.entity.Member;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface RoommateBoardRepositoryCustom {
    Slice<BoardBaseRow> search(BoardListDto.Request request, Pageable pageable, LocalDateTime endDate, @Nullable Long requesterId);
    Optional<BasicInfoRow> getBasicInfo(Long boardId);
    Page<MyRoommateBoardRow> findMyBoardList(Pageable page, Member member);
    Optional<EditFormRow> getEditRow(Long boardId);
    List<BoBoardListDto.Response.BoardInfo> findBackOfficeBoardList(Pageable pageable, BoBoardListDto.Request request);
    BoBoardDetailDto.Response findBackOffcieBoard(Long id);
}
