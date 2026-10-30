package org.example.knockin.life.repository.Impl;

import static org.example.knockin.life.entity.QLifePattern.lifePattern;
import static org.example.knockin.life.entity.QLifePatternInformation.lifePatternInformation;
import static org.example.knockin.life.entity.QPreferenceCondition.preferenceCondition;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.knockin.board.dto.BoardDetailDto;
import org.example.knockin.meta.entity.QFile;
import org.example.knockin.life.entity.QLifePatternFile;
import org.example.knockin.life.repository.PreferenceConditionRepositoryCustom;
import org.example.knockin.life.repository.row.MatchingPreferenceConditionRow;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PreferenceConditionRepositoryImpl implements PreferenceConditionRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<BoardDetailDto.Response.Condition> getConditionDtoByMemberId(Long memberId) {
        QLifePatternFile selectedPatternFile = new QLifePatternFile("selectedEditConditionPatternFile");
        QLifePatternFile latestPatternFile = new QLifePatternFile("latestEditConditionPatternFile");
        QFile patternImageFile = new QFile("editConditionPatternImageFile");

        return jpaQueryFactory
                .select(Projections.constructor(
                        BoardDetailDto.Response.Condition.class,
                        preferenceCondition.id,
                        lifePattern.name,
                        lifePatternInformation.dvalue,
                        lifePatternInformation.description,
                        lifePattern.dtype,
                        patternImageFile.savedFileName
                ))
                .from(preferenceCondition)
                .join(preferenceCondition.lifePatternInformation, lifePatternInformation)
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
                .where(preferenceCondition.memberId.eq(memberId))
                .fetch();
    }

    @Override
    public List<MatchingPreferenceConditionRow> findAllPreferenceConditionByMemberIdIn(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }

        QLifePatternFile selectedPatternFile = new QLifePatternFile("selectedMatchingConditionPatternFile");
        QLifePatternFile latestPatternFile = new QLifePatternFile("latestMatchingConditionPatternFile");
        QFile patternImageFile = new QFile("matchingConditionPatternImageFile");

        return jpaQueryFactory
                .select(Projections.constructor(
                        MatchingPreferenceConditionRow.class,
                        preferenceCondition.memberId,
                        preferenceCondition.id,
                        lifePattern.id,
                        lifePatternInformation.id,
                        lifePattern.name,
                        lifePatternInformation.dvalue,
                        lifePatternInformation.description,
                        lifePattern.dtype,
                        patternImageFile.savedFileName
                ))
                .from(preferenceCondition)
                .join(preferenceCondition.lifePatternInformation, lifePatternInformation)
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
                        preferenceCondition.memberId.in(memberIds),
                        lifePattern.isDeleted.isFalse()
                )
                .orderBy(lifePattern.sort.asc(), preferenceCondition.id.asc())
                .fetch();
    }

    @Override
    public List<Long> findLifeInformationIdByMemberId(Long memberId) {
        return jpaQueryFactory
                .select(lifePatternInformation.id)
                .distinct()
                .from(preferenceCondition)
                .join(preferenceCondition.lifePatternInformation, lifePatternInformation)
                .where(preferenceCondition.memberId.eq(memberId))
                .fetch();
    }
}
