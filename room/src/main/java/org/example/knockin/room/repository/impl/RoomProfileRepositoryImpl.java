package org.example.knockin.room.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.example.knockin.room.repository.RoomProfileRepositoryCustom;
import org.springframework.stereotype.Repository;

import static org.example.knockin.room.entity.QRoomProfile.roomProfile;
import static org.example.knockin.room.entity.QOfferRoomType.offerRoomType;
import static org.example.knockin.room.entity.QSeekerRoomType.seekerRoomType;
import static org.example.knockin.room.entity.QRoomSeekerProfileRegion.roomSeekerProfileRegion;

@Repository
@RequiredArgsConstructor
public class RoomProfileRepositoryImpl implements RoomProfileRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public boolean isExsitRoomProfile(Long memberId) {
        Long result = jpaQueryFactory.select(roomProfile.id).from(roomProfile).where(roomProfile.memberId.eq(memberId)).fetchFirst();
        return result != null;
    }

    @Override
    public void deleteForMembers(List<Long> memberIds) {
        if (memberIds.isEmpty()) return;

        List<Long> profileIds = jpaQueryFactory.select(roomProfile.id).from(roomProfile)
                .where(roomProfile.memberId.in(memberIds)).fetch();
        if (profileIds.isEmpty()) return;

        jpaQueryFactory.delete(offerRoomType)
                .where(offerRoomType.roomOfferProfile.id.in(profileIds)).execute();
        jpaQueryFactory.delete(seekerRoomType)
                .where(seekerRoomType.roomSeekerProfile.id.in(profileIds)).execute();
        jpaQueryFactory.delete(roomSeekerProfileRegion)
                .where(roomSeekerProfileRegion.roomSeekerProfile.id.in(profileIds)).execute();
        jpaQueryFactory.delete(roomProfile).where(roomProfile.id.in(profileIds)).execute();
    }
}
