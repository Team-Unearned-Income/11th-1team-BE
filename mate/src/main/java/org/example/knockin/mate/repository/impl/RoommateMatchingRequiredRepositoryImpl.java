package org.example.knockin.mate.repository.impl;

import com.querydsl.core.types.Projections;
import static org.example.knockin.mate.entity.QRoommateScore.roommateScore;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import static org.example.knockin.mate.entity.QRoommateMatchingRequired.roommateMatchingRequired;

import org.example.knockin.mate.dto.RoommateRequestDto.RoommateMatchingRequiredInfo;
import org.example.knockin.mate.entity.RoommateMatchingRequired;
import org.example.knockin.mate.repository.RoommateMatchingRequiredRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RoommateMatchingRequiredRepositoryImpl implements RoommateMatchingRequiredRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public void deleteForMembers(List<Long> memberIds, List<Long> chatRoomIds, List<Long> chatScoreIds) {
        if (memberIds.isEmpty()) return;

        jpaQueryFactory.delete(roommateScore).where(roommateScore.chattingScoreId.in(chatScoreIds)).execute();
        jpaQueryFactory.delete(roommateMatchingRequired)
                .where(roommateMatchingRequired.requester.id.in(memberIds)
                        .or(roommateMatchingRequired.requestee.id.in(memberIds))
                        .or(roommateMatchingRequired.chattingRoomId.in(chatRoomIds)))
                .execute();
    }

    @Override
    public Optional<RoommateMatchingRequired> findLatest(Long chattingRoomId) {
        return Optional.ofNullable(
                jpaQueryFactory
                        .select(roommateMatchingRequired)
                        .from(roommateMatchingRequired)
                        .where(
                                roommateMatchingRequired.chattingRoomId.eq(chattingRoomId)
                        )
                        .orderBy(roommateMatchingRequired.id.desc())
                        .fetchFirst()
        );
    }

    @Override
    public List<RoommateMatchingRequiredInfo> findRequiredDto(Long chattingRoomId) {
        return jpaQueryFactory
                .select(Projections.constructor(
                        RoommateMatchingRequiredInfo.class,
                        roommateMatchingRequired.id,
                        roommateMatchingRequired.requester.id,
                        roommateMatchingRequired.requestee.id,
                        roommateMatchingRequired.status,
                        roommateMatchingRequired.createdAt,
                        roommateMatchingRequired.updatedAt
                ))
                .from(roommateMatchingRequired)
                .where(roommateMatchingRequired.chattingRoomId.eq(chattingRoomId))
                .fetch();
    }

    @Override
    public Page<RoommateMatchingRequired> findMyRequiredList(Long memberId, Pageable pageable) {
        List<RoommateMatchingRequired> content = jpaQueryFactory
                .select(roommateMatchingRequired)
                .from(roommateMatchingRequired)
                .where(roommateMatchingRequired.requester.id.eq(memberId)
                        .or(roommateMatchingRequired.requestee.id.eq(memberId)))
                .orderBy(roommateMatchingRequired.id.desc())
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .fetch();

        Long count = jpaQueryFactory
                .select(roommateMatchingRequired.count())
                .from(roommateMatchingRequired)
                .where(roommateMatchingRequired.requester.id.eq(memberId)
                        .or(roommateMatchingRequired.requestee.id.eq(memberId)))
                .fetchOne();

        return new PageImpl<>(content, pageable, count);
    }
}
