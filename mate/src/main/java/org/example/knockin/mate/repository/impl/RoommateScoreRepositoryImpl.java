package org.example.knockin.mate.repository.impl;

import static org.example.knockin.mate.entity.QRoommateScore.roommateScore;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.knockin.mate.entity.RoommateScore;
import org.example.knockin.mate.repository.RoommateScoreRepositoryCustom;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RoommateScoreRepositoryImpl implements RoommateScoreRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Optional<RoommateScore> findOneByMyRoommateIdAndChattingScoreId(Long myRoommateId, Long chattingScoreId) {
        return Optional.ofNullable(
                jpaQueryFactory
                        .selectFrom(roommateScore)
                        .where(
                                roommateScore.myRoommate.id.eq(myRoommateId),
                                roommateScore.chattingScoreId.eq(chattingScoreId)
                        )
                        .fetchOne()
        );
    }
}
