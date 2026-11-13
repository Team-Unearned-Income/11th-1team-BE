package org.example.knockin.board.repository.impl;

import static org.example.knockin.board.entity.QRoommateBoardFile.roommateBoardFile;
import static org.example.knockin.meta.entity.QFile.file;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.knockin.board.dto.BoardDetailDto.Response.FileDetailDto;
import org.example.knockin.board.repository.RoommateBoardFileRepositoryCustom;
import org.example.knockin.board.repository.row.BoardThumbnailRow;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RoommateBoardFileRepositoryImpl implements RoommateBoardFileRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<FileDetailDto> getFileDetailDtoByBoardId(Long boardId) {
        return jpaQueryFactory
                .select(Projections.constructor(
                        FileDetailDto.class,
                        roommateBoardFile.id,
                        file.savedFileName
                ))
                .from(roommateBoardFile)
                .join(file).on(roommateBoardFile.fileId.eq(file.id))
                .where(
                        roommateBoardFile.roommateBoard.id.eq(boardId),
                        file.isDeleted.isFalse()
                )
                .orderBy(roommateBoardFile.isThumbnail.desc(), roommateBoardFile.id.asc())
                .limit(10)
                .fetch();
    }

    @Override
    public List<BoardThumbnailRow> findThumbnailsByBoardIds(List<Long> boardIds) {
        return jpaQueryFactory
                .select(Projections.constructor(
                        BoardThumbnailRow.class,
                        roommateBoardFile.roommateBoard.id,
                        file.savedFileName
                ))
                .from(roommateBoardFile)
                .join(file).on(roommateBoardFile.fileId.eq(file.id))
                .where(
                        roommateBoardFile.roommateBoard.id.in(boardIds),
                        roommateBoardFile.isThumbnail.isTrue(),
                        file.isDeleted.isFalse()
                )
                .fetch();
    }
}
