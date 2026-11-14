package org.example.knockin.board.repository.impl;

import static org.example.knockin.board.entity.QRoommateBoardInterest.roommateBoardInterest;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.knockin.board.repository.RoommateBoardInterestRepositoryCustom;
import org.example.knockin.board.repository.row.BoardInterestCountRow;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RoommateBoardInterestRepositoryImpl implements RoommateBoardInterestRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<Long> findActiveBoardIdsByMemberIdAndBoardIds(Long memberId, Collection<Long> boardIds) {
        if (boardIds.isEmpty()) {
            return List.of();
        }

        return jpaQueryFactory
                .select(roommateBoardInterest.roommateBoard.id)
                .from(roommateBoardInterest)
                .where(
                        roommateBoardInterest.memberId.eq(memberId),
                        roommateBoardInterest.roommateBoard.id.in(boardIds),
                        roommateBoardInterest.isDeleted.isFalse()
                )
                .fetch();
    }

    @Override
    public List<BoardInterestCountRow> findActiveInterestCountsByBoardIds(List<Long> boardIds) {
        if (boardIds.isEmpty()) return List.of();

        return jpaQueryFactory
                .select(Projections.constructor(
                        BoardInterestCountRow.class,
                        roommateBoardInterest.roommateBoard.id,
                        roommateBoardInterest.count()
                ))
                .from(roommateBoardInterest)
                .where(
                        roommateBoardInterest.roommateBoard.id.in(boardIds),
                        roommateBoardInterest.isDeleted.isFalse()
                )
                .groupBy(roommateBoardInterest.roommateBoard.id)
                .fetch();
    }
}
