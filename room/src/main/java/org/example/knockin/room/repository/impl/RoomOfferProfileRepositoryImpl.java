package org.example.knockin.room.repository.impl;

import static org.example.knockin.room.entity.QOfferRoomType.offerRoomType;
import static org.example.knockin.room.entity.QRoomOfferProfile.roomOfferProfile;
import static org.example.knockin.room.entity.QRoomType.roomType;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.knockin.room.entity.QOfferRoomType;
import org.example.knockin.meta.entity.QRegion;
import org.example.knockin.room.entity.QRoomProfile;
import org.example.knockin.room.repository.RoomOfferProfileRepositoryCustom;
import org.example.knockin.room.repository.row.MatchingOfferProfileRow;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RoomOfferProfileRepositoryImpl implements RoomOfferProfileRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<MatchingOfferProfileRow> findAllOfferProfileByMemberIdIn(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }

        QRoomProfile latestRoomProfile = new QRoomProfile("latestRoomProfile");
        QOfferRoomType latestOfferRoomType = new QOfferRoomType("latestOfferRoomType");

        QRegion offerRegion = new QRegion("offerRegion");
        QRegion parentRegion = new QRegion("parentRegion");
        QRegion grandParentRegion = new QRegion("grandParentRegion");

        return jpaQueryFactory
                .select(Projections.constructor(
                        MatchingOfferProfileRow.class,
                        roomOfferProfile.memberId,
                        roomOfferProfile.deposit,
                        roomOfferProfile.monthlyRent,
                        offerRegion.name,
                        parentRegion.name,
                        grandParentRegion.name,
                        roomType.name
                ))
                .from(roomOfferProfile)
                .join(offerRegion).on(roomOfferProfile.regionId.eq(offerRegion.id))
                .leftJoin(offerRegion.parent, parentRegion)
                .leftJoin(parentRegion.parent, grandParentRegion)
                .join(offerRoomType)
                .on(
                        offerRoomType.roomOfferProfile.id.eq(roomOfferProfile.id),
                        offerRoomType.id.eq(
                                JPAExpressions
                                        .select(latestOfferRoomType.id.max())
                                        .from(latestOfferRoomType)
                                        .where(latestOfferRoomType.roomOfferProfile.id.eq(roomOfferProfile.id))
                        )
                )
                .join(offerRoomType.roomType, roomType)
                .where(
                        roomOfferProfile.memberId.in(memberIds),
                        roomOfferProfile.id.eq(
                                JPAExpressions
                                        .select(latestRoomProfile.id.max())
                                        .from(latestRoomProfile)
                                        .where(latestRoomProfile.memberId.eq(roomOfferProfile.memberId))
                        )
                )
                .fetch();
    }
}
