package org.example.knockin.life.repository.Impl;

import static org.example.knockin.life.entity.QLifePattern.lifePattern;
import static org.example.knockin.life.entity.QLifePatternInformation.lifePatternInformation;
import static org.example.knockin.life.entity.QMemberLifePattern.memberLifePattern;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.knockin.board.dto.BoardDetailDto;
import org.example.knockin.meta.entity.QFile;
import org.example.knockin.life.entity.QLifePatternFile;
import org.example.knockin.life.repository.MemberLifePatternRepositoryCustom;
import org.example.knockin.life.repository.row.MatchingLifestyleRow;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberLifePatternRepositoryImpl implements MemberLifePatternRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<BoardDetailDto.Response.Lifestyle> getLifeStyleDto(Long memberId) {
        QLifePatternFile selectedPatternFile = new QLifePatternFile("selectedEditLifePatternFile");
        QLifePatternFile latestPatternFile = new QLifePatternFile("latestEditLifePatternFile");
        QFile patternImageFile = new QFile("editLifePatternImageFile");

        return jpaQueryFactory
                .select(Projections.constructor(
                        BoardDetailDto.Response.Lifestyle.class,
                        memberLifePattern.id,
                        lifePattern.name,
                        lifePatternInformation.dvalue,
                        lifePatternInformation.description,
                        lifePattern.dtype,
                        patternImageFile.savedFileName
                ))
                .from(memberLifePattern)
                .join(memberLifePattern.lifePatternInformation, lifePatternInformation)
                .join(lifePatternInformation.lifePattern, lifePattern)
                .leftJoin(selectedPatternFile)
                .on(selectedPatternFile.id.eq(
                        JPAExpressions
                                .select(latestPatternFile.id.max())
                                .from(latestPatternFile)
                                .where(latestPatternFile.lifePattern.eq(lifePattern))
                ))
                .leftJoin(patternImageFile)
                .on(selectedPatternFile.fileId.eq(patternImageFile.id), patternImageFile.isDeleted.isFalse())
                .where(memberLifePattern.memberId.eq(memberId))
                .orderBy(lifePattern.sort.asc())
                .fetch();
    }

    @Override
    public boolean isExsitLifeStyle(Long memberId) {
        Long result = jpaQueryFactory.select(memberLifePattern.id).from(memberLifePattern).where(memberLifePattern.memberId.eq(memberId)).fetchFirst();
        return result != null;
    }

    @Override
    public List<MatchingLifestyleRow> findAllLifestyleByMemberIdIn(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }

        QLifePatternFile selectedPatternFile = new QLifePatternFile("selectedMatchingLifePatternFile");
        QLifePatternFile latestPatternFile = new QLifePatternFile("latestMatchingLifePatternFile");
        QFile patternImageFile = new QFile("matchingLifePatternImageFile");

        return jpaQueryFactory
                .select(Projections.constructor(
                        MatchingLifestyleRow.class,
                        memberLifePattern.memberId,
                        memberLifePattern.id,
                        lifePattern.id,
                        lifePatternInformation.id,
                        lifePattern.name,
                        lifePatternInformation.dvalue,
                        lifePatternInformation.description,
                        lifePattern.dtype,
                        patternImageFile.savedFileName
                ))
                .from(memberLifePattern)
                .join(memberLifePattern.lifePatternInformation, lifePatternInformation)
                .join(lifePatternInformation.lifePattern, lifePattern)
                .leftJoin(selectedPatternFile)
                .on(selectedPatternFile.id.eq(
                        JPAExpressions
                                .select(latestPatternFile.id.max())
                                .from(latestPatternFile)
                                .where(latestPatternFile.lifePattern.eq(lifePattern))
                ))
                .leftJoin(patternImageFile)
                .on(selectedPatternFile.fileId.eq(patternImageFile.id), patternImageFile.isDeleted.isFalse())
                .where(
                        memberLifePattern.memberId.in(memberIds),
                        lifePattern.isDeleted.isFalse()
                )
                .orderBy(lifePattern.sort.asc(), memberLifePattern.id.asc())
                .fetch();
    }
}
