package org.example.knockin.room.repository.impl;

import static org.example.knockin.room.entity.QRoomType.roomType;
import static org.example.knockin.room.entity.QSeekerRoomType.seekerRoomType;
import static org.example.knockin.room.entity.QRoomSeekerProfile.roomSeekerProfile;
import static org.example.knockin.room.entity.QRoomSeekerProfileRegion.roomSeekerProfileRegion;


import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.knockin.meta.entity.QRegion;
import org.example.knockin.room.entity.QRoomProfile;
import org.example.knockin.room.repository.RoomSeekerProfileRepositoryCustom;
import org.example.knockin.room.repository.row.MatchingSeekerProfileRow;
import org.example.knockin.room.repository.row.MatchingSeekerRegionRow;
import org.example.knockin.room.repository.row.MatchingSeekerRoomTypeRow;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RoomSeekerProfileRepositoryImpl implements RoomSeekerProfileRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<MatchingSeekerProfileRow> findAllSeekerProfileByMemberIdIn(List<Long> memberIds) {

        QRoomProfile latestRoomProfile = new QRoomProfile("latestRoomProfile");

        return jpaQueryFactory
                .select(Projections.constructor(
                        MatchingSeekerProfileRow.class,
                        roomSeekerProfile.memberId,
                        roomSeekerProfile.minDeposit,
                        roomSeekerProfile.maxDeposit,
                        roomSeekerProfile.minMonthlyRent,
                        roomSeekerProfile.maxMonthlyRent
                ))
                .from(roomSeekerProfile)
                .where(
                        roomSeekerProfile.memberId.in(memberIds),
                        roomSeekerProfile.id.eq(
                                JPAExpressions
                                        .select(latestRoomProfile.id.max())
                                        .from(latestRoomProfile)
                                        .where(latestRoomProfile.memberId.eq(roomSeekerProfile.memberId))
                        )
                )
                .fetch();
    }

    @Override
    public List<MatchingSeekerRegionRow> findAllSeekerRegionByMemberIdIn(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }

        QRoomProfile latestRoomProfile = new QRoomProfile("latestRoomProfile");
        QRegion seekerRegion = new QRegion("seekerRegion");
        QRegion parentRegion = new QRegion("parentRegion");
        QRegion grandParentRegion = new QRegion("grandParentRegion");

        return jpaQueryFactory
                .select(Projections.constructor(
                        MatchingSeekerRegionRow.class,
                        roomSeekerProfile.memberId,
                        seekerRegion.name,
                        parentRegion.name,
                        grandParentRegion.name
                ))
                .from(roomSeekerProfileRegion)
                .join(roomSeekerProfileRegion.roomSeekerProfile, roomSeekerProfile)
                .join(seekerRegion).on(roomSeekerProfileRegion.regionId.eq(seekerRegion.id))
                .leftJoin(seekerRegion.parent, parentRegion)
                .leftJoin(parentRegion.parent, grandParentRegion)
                .where(
                        roomSeekerProfile.memberId.in(memberIds),
                        roomSeekerProfile.id.eq(
                                JPAExpressions
                                        .select(latestRoomProfile.id.max())
                                        .from(latestRoomProfile)
                                        .where(latestRoomProfile.memberId.eq(roomSeekerProfile.memberId))
                        )
                )
                .fetch();
    }

    @Override
    public List<MatchingSeekerRoomTypeRow> findAllSeekerRoomTypeByMemberIdIn(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }

        QRoomProfile latestRoomProfile = new QRoomProfile("latestRoomProfile");

        return jpaQueryFactory
                .select(Projections.constructor(
                        MatchingSeekerRoomTypeRow.class,
                        roomSeekerProfile.memberId,
                        roomType.name
                ))
                .from(seekerRoomType)
                .join(seekerRoomType.roomSeekerProfile, roomSeekerProfile)
                .join(seekerRoomType.roomType, roomType)
                .where(
                        roomSeekerProfile.memberId.in(memberIds),
                        roomSeekerProfile.id.eq(
                                JPAExpressions
                                        .select(latestRoomProfile.id.max())
                                        .from(latestRoomProfile)
                                        .where(latestRoomProfile.memberId.eq(roomSeekerProfile.memberId))
                        )
                )
                .fetch();
    }
}
