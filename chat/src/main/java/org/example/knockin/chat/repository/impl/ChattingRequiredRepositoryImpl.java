package org.example.knockin.chat.repository.impl;

import static org.example.knockin.member.entity.QBasicInformation.basicInformation;
import static org.example.knockin.chat.entity.QChattingRequired.chattingRequired;
import static org.example.knockin.chat.entity.QChatRoomMember.chatRoomMember;
import static org.example.knockin.chat.entity.QChatRoomMessage.chatRoomMessage;
import static org.example.knockin.board.entity.QRoommateBoard.roommateBoard;
import com.querydsl.jpa.JPQLSubQuery;
import static org.example.knockin.chat.entity.QChattingRoom.chattingRoom;
import static org.example.knockin.chat.entity.QChattingScore.chattingScore;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.knockin.chat.entity.ChattingRequired;
import org.example.knockin.chat.entity.ChattingRequiredStatus;
import org.example.knockin.chat.repository.ChattingRequiredRepositoryCustom;
import org.example.knockin.chat.repository.row.ChatRequestListRow;
import org.example.knockin.member.entity.QBasicInformation;
import org.example.knockin.member.entity.QMember;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChattingRequiredRepositoryImpl implements ChattingRequiredRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<Long> findIdsForMembers(List<Long> memberIds) {
        if (memberIds.isEmpty()) return List.of();
        return jpaQueryFactory.select(chattingRequired.id).from(chattingRequired)
                .where(chattingRequired.requesterId.in(memberIds).or(chattingRequired.requesteeId.in(memberIds)))
                .fetch();
    }

    @Override
    public List<Long> findRoomIdsByRequestIds(List<Long> requestIds) {
        if (requestIds.isEmpty()) return List.of();
        return jpaQueryFactory.select(chattingRoom.id).from(chattingRoom)
                .where(chattingRoom.chattingRequired.id.in(requestIds)).fetch();
    }

    @Override
    public List<Long> findScoreIdsByRequestIds(List<Long> requestIds) {
        if (requestIds.isEmpty()) return List.of();
        return jpaQueryFactory.select(chattingScore.id).from(chattingScore)
                .where(chattingScore.chattingRequired.id.in(requestIds)).fetch();
    }

    @Override
    public void deleteForMembers(List<Long> memberIds) {
        if (memberIds.isEmpty()) return;

        JPQLSubQuery<Long> boardIds = JPAExpressions.select(roommateBoard.id).from(roommateBoard)
                .where(roommateBoard.memberId.in(memberIds));
        jpaQueryFactory.update(chattingRequired)
                .setNull(chattingRequired.roommateBoardId)
                .where(chattingRequired.roommateBoardId.in(boardIds)).execute();
        jpaQueryFactory.delete(chatRoomMessage).where(chatRoomMessage.memberId.in(memberIds)).execute();
        jpaQueryFactory.delete(chatRoomMember).where(chatRoomMember.memberId.in(memberIds)).execute();
        jpaQueryFactory.delete(chattingRequired)
                .where(chattingRequired.requesterId.in(memberIds).or(chattingRequired.requesteeId.in(memberIds)))
                .execute();
    }

    @Override
    public boolean existsBetweenMembers(Long memberAId, Long memberBId) {
        Integer result = jpaQueryFactory
                .selectOne()
                .from(chattingRequired)
                .where(
                        chattingRequired.requesterId.eq(memberAId).and(chattingRequired.requesteeId.eq(memberBId))
                                .or(chattingRequired.requesterId.eq(memberBId).and(chattingRequired.requesteeId.eq(memberAId)))
                )
                .fetchFirst();

        return result != null;
    }

    @Override
    public Optional<ChattingRequired> findLatest(Long memberAId, Long memberBId) {
        return Optional.ofNullable(jpaQueryFactory
                .selectFrom(chattingRequired)
                .where(
                        chattingRequired.requesterId.eq(memberAId).and(chattingRequired.requesteeId.eq(memberBId))
                                .or(chattingRequired.requesterId.eq(memberBId).and(chattingRequired.requesteeId.eq(memberAId)))
                )
                .orderBy(chattingRequired.id.desc())
                .fetchFirst());
    }

    @Override
    public List<ChatRequestListRow> findAllPendingByRequesteeId(Long requesteeId) {
        QMember requester = new QMember("requester");
        QBasicInformation basicInformationSub = new QBasicInformation("basicInformationSub");

        return jpaQueryFactory
                .select(Projections.constructor(
                        ChatRequestListRow.class,
                        chattingRequired.id,
                        chattingRequired.status,
                        requester.id,
                        basicInformation.name,
                        basicInformation.birth,
                        basicInformation.gender,
                        chattingRequired.createdAt
                ))
                .from(chattingRequired)
                .join(requester).on(chattingRequired.requesterId.eq(requester.id))
                .leftJoin(basicInformation)
                .on(basicInformation.id.eq(
                        JPAExpressions
                                .select(basicInformationSub.id.max())
                                .from(basicInformationSub)
                                .where(basicInformationSub.member.eq(requester))
                ))
                .where(
                        chattingRequired.requesteeId.eq(requesteeId),
                        chattingRequired.status.eq(ChattingRequiredStatus.PENDING)
                )
                .orderBy(chattingRequired.createdAt.desc(), chattingRequired.id.desc())
                .fetch();
    }
}
