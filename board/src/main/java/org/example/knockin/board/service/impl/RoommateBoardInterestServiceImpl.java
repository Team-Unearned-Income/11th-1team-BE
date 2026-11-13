package org.example.knockin.board.service.impl;

import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.knockin.board.entity.RoommateBoard;
import org.example.knockin.board.entity.RoommateBoardInterest;
import org.example.knockin.board.repository.RoommateBoardInterestRepository;
import org.example.knockin.board.repository.row.BoardInterestCountRow;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoommateBoardInterestServiceImpl {
    private final RoommateBoardInterestRepository roommateBoardInterestRepository;

    public boolean existsActiveByBoardIdAndMemberId(Long boardId, Long memberId) {
        return roommateBoardInterestRepository.existsByRoommateBoardIdAndMemberIdAndIsDeletedIsFalse(boardId, memberId);
    }

    public List<Long> findActiveBoardIdsByMemberIdAndBoardIds(Long memberId, Collection<Long> boardIds) {
        if (boardIds.isEmpty()) {
            return List.of();
        }
        return roommateBoardInterestRepository.findActiveBoardIdsByMemberIdAndBoardIds(memberId, boardIds);
    }

    public void toggle(Long memberId, RoommateBoard roommateBoard) {
        roommateBoardInterestRepository.findByRoommateBoardAndMemberId(roommateBoard, memberId)
                .ifPresentOrElse(
                        RoommateBoardInterest::likeToggle,
                        () -> save(memberId, roommateBoard)
                );
    }

    private void save(Long memberId, RoommateBoard roommateBoard) {
        RoommateBoardInterest roommateBoardInterest = RoommateBoardInterest.builder()
                .memberId(memberId)
                .roommateBoard(roommateBoard)
                .isDeleted(false)
                .build();
        roommateBoardInterestRepository.save(roommateBoardInterest);
    }

    public List<BoardInterestCountRow> findActiveInterestCountsByBoardIds(List<Long> boardIds) {
        return roommateBoardInterestRepository.findActiveInterestCountsByBoardIds(boardIds);
    }
}
