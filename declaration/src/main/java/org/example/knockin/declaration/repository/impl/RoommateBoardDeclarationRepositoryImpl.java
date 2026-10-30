package org.example.knockin.declaration.repository.impl;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.knockin.declaration.repository.RoommateBoardDeclarationRepositoryCustom;
import org.example.knockin.declaration.dto.BoReportDoneListDto;
import org.example.knockin.declaration.dto.BoReportWaitListDto;
import org.example.knockin.declaration.entity.DeclarationType;
import org.example.knockin.global.util.ReportType;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.example.knockin.member.entity.QMember.member;
import static org.example.knockin.declaration.entity.QRoommateBoardDeclaration.roommateBoardDeclaration;
import static org.example.knockin.member.entity.QBasicInformation.basicInformation;

@Repository
@RequiredArgsConstructor
public class RoommateBoardDeclarationRepositoryImpl implements RoommateBoardDeclarationRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<BoReportWaitListDto.Response.ReportInfo> findReportWaitList(Pageable pageable) {
        return jpaQueryFactory
                .select(Projections.fields(BoReportWaitListDto.Response.ReportInfo.class,
                        roommateBoardDeclaration.id,
                        ExpressionUtils.as(Expressions.constant(ReportType.BOARD.name()), "type"),
                        basicInformation.name.as("reporter"),
                        roommateBoardDeclaration.memberId.as("reporterId"),
                        roommateBoardDeclaration.roommateBoardId.as("reportedId"),
                        roommateBoardDeclaration.reason,
                        roommateBoardDeclaration.createdAt
                ))
                .from(roommateBoardDeclaration)
                .join(member).on(member.id.eq(roommateBoardDeclaration.memberId))
                .leftJoin(basicInformation)
                .on(basicInformation.member.id.eq(roommateBoardDeclaration.memberId))
                .where(roommateBoardDeclaration.declarationType.eq(DeclarationType.PENDING))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<BoReportDoneListDto.Response.ReportInfo> findReportDoneList(Pageable pageable) {
        return jpaQueryFactory
                .select(Projections.fields(BoReportDoneListDto.Response.ReportInfo.class,
                        roommateBoardDeclaration.id,
                        ExpressionUtils.as(Expressions.constant(ReportType.BOARD.name()), "type"),
                        basicInformation.name.as("reporter"),
                        roommateBoardDeclaration.createdAt,
                        roommateBoardDeclaration.declarationType
                ))
                .from(roommateBoardDeclaration)
                .join(member).on(member.id.eq(roommateBoardDeclaration.memberId))
                .leftJoin(basicInformation)
                .on(basicInformation.member.id.eq(roommateBoardDeclaration.memberId))
                .where(roommateBoardDeclaration.declarationType.ne(DeclarationType.PENDING))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }
}