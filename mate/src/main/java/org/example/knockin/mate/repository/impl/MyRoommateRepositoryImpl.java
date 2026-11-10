package org.example.knockin.mate.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.QMember;
import org.example.knockin.mate.entity.MyRoommate;
import org.example.knockin.mate.repository.MyRoommateRepositoryCustom;
import org.springframework.stereotype.Repository;

import static org.example.knockin.mate.entity.QMyRoommate.myRoommate;
import static org.example.knockin.mate.entity.QRoommateMatchingRequired.roommateMatchingRequired;

@Repository
@RequiredArgsConstructor
public class MyRoommateRepositoryImpl implements MyRoommateRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public boolean isExistRoomMate(Member member) {
        Integer fetchOne = jpaQueryFactory.selectOne().from(myRoommate)
                .join(myRoommate.roommateMatchingRequired, roommateMatchingRequired)
                .where(
                        roommateMatchingRequired.requestee.eq(member).or(roommateMatchingRequired.requester.eq(member)),
                        myRoommate.isDeleted.isFalse()
                )
                .fetchFirst();

        return fetchOne != null;
    }

    @Override
    public Optional<MyRoommate> findWithRequiredByMemberId(Long memberId) {
        return Optional.ofNullable(
                jpaQueryFactory
                        .select(myRoommate)
                        .from(myRoommate)
                        .join(myRoommate.roommateMatchingRequired, roommateMatchingRequired).fetchJoin()
                        .where(
                                roommateMatchingRequired.requestee.id.eq(memberId)
                                        .or(roommateMatchingRequired.requester.id.eq(memberId)),
                                myRoommate.isDeleted.isFalse()
                        )
                        .fetchFirst()
        );
    }

    @Override
    public Optional<MyRoommate> findWithRequiredAndMembersByMemberId(Long memberId) {
        QMember requestee = new QMember("requestee");
        QMember requester = new QMember("requester");

        return Optional.ofNullable(
                jpaQueryFactory
                        .select(myRoommate)
                        .from(myRoommate)
                        .join(myRoommate.roommateMatchingRequired, roommateMatchingRequired).fetchJoin()
                        .leftJoin(roommateMatchingRequired.requester, requester).fetchJoin()
                        .leftJoin(roommateMatchingRequired.requestee, requestee).fetchJoin()
                        .where(
                                roommateMatchingRequired.requestee.id.eq(memberId)
                                        .or(roommateMatchingRequired.requester.id.eq(memberId)),
                                myRoommate.isDeleted.isFalse()
                        )
                        .fetchFirst()
        );
    }
}
