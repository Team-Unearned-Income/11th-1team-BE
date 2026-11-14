package org.example.knockin.board.repository;

import java.util.List;
import java.util.Optional;
import org.example.knockin.board.entity.RoommateBoard;
import org.example.knockin.board.entity.RoommateBoardInterest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoommateBoardInterestRepository extends JpaRepository<RoommateBoardInterest, Long>, RoommateBoardInterestRepositoryCustom {
    Optional<RoommateBoardInterest> findByRoommateBoardAndMemberId(RoommateBoard roommateBoard, Long memberId);

    List<RoommateBoardInterest> findAllByRoommateBoardIdAndMemberId(Long roommateBoardId, Long memberId);

    boolean existsByRoommateBoardIdAndMemberIdAndIsDeletedIsFalse(Long roommateBoardId, Long memberId);
}
