package org.example.knockin.chat.repository.impl;

import static org.example.knockin.chat.entity.QChattingRequired.chattingRequired;
import static org.example.knockin.chat.entity.QChattingRoom.chattingRoom;
import static org.example.knockin.member.entity.QBasicInformationFile.basicInformationFile;
import static org.example.knockin.member.entity.QBasicInformation.basicInformation;
import static org.example.knockin.mate.entity.QMyRoommate.myRoommate;

import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.knockin.chat.entity.QChatRoomMember;
import org.example.knockin.chat.entity.QChatRoomMessage;
import org.example.knockin.chat.repository.ChattingRoomRepositoryCustom;
import org.example.knockin.chat.repository.row.ChatRoomListRow;
import org.example.knockin.mate.entity.QRoommateMatchingRequired;
import org.example.knockin.member.entity.QBasicInformation;
import org.example.knockin.member.entity.QBasicInformationFile;
import org.example.knockin.member.entity.QMember;
import org.example.knockin.meta.entity.QFile;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChattingRoomRepositoryImpl implements ChattingRoomRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<ChatRoomListRow> findListRowsByMemberId(Long memberId) {
        QMember viewerMember = new QMember("viewerMember");
        QMember opponentMember = new QMember("opponentMember");
        QChatRoomMember viewerRoomMember = new QChatRoomMember("viewerRoomMember");
        QChatRoomMember opponentRoomMember = new QChatRoomMember("opponentRoomMember");
        QBasicInformation basicInformationSub = new QBasicInformation("basicInformationSub");
        QBasicInformationFile basicInformationFileSub = new QBasicInformationFile("basicInformationFileSub");
        QFile profileImageFile = new QFile("profileImageFile");
        QChatRoomMessage latestMessage = new QChatRoomMessage("latestMessage");
        QChatRoomMessage latestMessageSub = new QChatRoomMessage("latestMessageSub");
        QRoommateMatchingRequired latestRoommateRequired = new QRoommateMatchingRequired("latestRoommateRequired");
        QRoommateMatchingRequired latestRoommateRequiredSub = new QRoommateMatchingRequired("latestRoommateRequiredSub");

        return jpaQueryFactory
                .select(Projections.constructor(
                        ChatRoomListRow.class,
                        chattingRoom.id,
                        opponentMember.id,
                        basicInformation.name,
                        profileImageFile.savedFileName,
                        chattingRoom.createdAt,
                        latestRoommateRequired.status,
                        new CaseBuilder()
                                .when(JPAExpressions
                                        .selectOne()
                                        .from(myRoommate)
                                        .where(
                                                myRoommate.roommateMatchingRequired.chattingRoom.eq(chattingRoom),
                                                myRoommate.isDeleted.isFalse()
                                        )
                                        .exists())
                                .then(true)
                                .otherwise(false),
                        latestMessage.contents,
                        latestMessage.createdAt
                ))
                .from(chattingRoom)
                .join(chattingRoom.chattingRequired, chattingRequired)
                .join(viewerRoomMember)
                .on(
                        viewerRoomMember.chattingRoom.eq(chattingRoom),
                        viewerRoomMember.member.id.eq(memberId),
                        viewerRoomMember.isLeft.isFalse()
                )
                .join(viewerRoomMember.member, viewerMember)
                .join(opponentRoomMember)
                .on(
                        opponentRoomMember.chattingRoom.eq(chattingRoom),
                        opponentRoomMember.member.id.ne(memberId)
                )
                .join(opponentRoomMember.member, opponentMember)
                .leftJoin(basicInformation)
                .on(basicInformation.id.eq(
                        JPAExpressions
                                .select(basicInformationSub.id.max())
                                .from(basicInformationSub)
                                .where(basicInformationSub.member.eq(opponentMember))
                ))
                .leftJoin(basicInformationFile)
                .on(basicInformationFile.id.eq(
                        JPAExpressions
                                .select(basicInformationFileSub.id.max())
                                .from(basicInformationFileSub)
                                .where(basicInformationFileSub.basicInformation.eq(basicInformation))
                ))
                .leftJoin(profileImageFile).on(basicInformationFile.fileId.eq(profileImageFile.id))
                .leftJoin(latestRoommateRequired)
                .on(latestRoommateRequired.id.eq(
                        JPAExpressions
                                .select(latestRoommateRequiredSub.id.max())
                                .from(latestRoommateRequiredSub)
                                .where(latestRoommateRequiredSub.chattingRoom.eq(chattingRoom))
                ))
                .leftJoin(latestMessage)
                .on(latestMessage.id.eq(
                        JPAExpressions
                                .select(latestMessageSub.id.max())
                                .from(latestMessageSub)
                                .where(latestMessageSub.chattingRoom.eq(chattingRoom))
                ))
                .orderBy(
                        latestMessage.createdAt.coalesce(chattingRoom.createdAt).desc(),
                        chattingRoom.id.desc()
                )
                .fetch();
    }

    @Override
    public boolean existsActiveRoomBetweenMembers(Long memberAId, Long memberBId) {
        QChatRoomMember memberA = new QChatRoomMember("memberA");
        QChatRoomMember memberB = new QChatRoomMember("memberB");

        Integer result = jpaQueryFactory
                .selectOne()
                .from(memberA)
                .join(memberB)
                .on(
                        memberB.chattingRoom.eq(memberA.chattingRoom),
                        memberB.member.id.eq(memberBId),
                        memberB.isLeft.isFalse()
                )
                .where(
                        memberA.member.id.eq(memberAId),
                        memberA.isLeft.isFalse()
                )
                .fetchFirst();

        return result != null;
    }

    @Override
    public long countActiveRoomsByMemberId(Long memberId) {
        QChatRoomMember roomMember = new QChatRoomMember("roomMember");

        Long count = jpaQueryFactory
                .select(roomMember.chattingRoom.id.countDistinct())
                .from(roomMember)
                .where(
                        roomMember.member.id.eq(memberId),
                        roomMember.isLeft.isFalse()
                )
                .fetchOne();

        return count == null ? 0 : count;
    }
}
