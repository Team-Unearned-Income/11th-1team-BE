package org.example.knockin.declaration.repository.impl;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.knockin.declaration.dto.BoReportDoneListDto;
import org.example.knockin.declaration.dto.BoReportWaitListDto;
import org.example.knockin.declaration.entity.DeclarationType;
import org.example.knockin.global.util.ReportType;
import org.example.knockin.declaration.repository.MemberDeclarationRepositoryCustom;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.example.knockin.member.entity.QMember.member;
import static org.example.knockin.declaration.entity.QMemberDeclaration.memberDeclaration;
import static org.example.knockin.member.entity.QBasicInformation.basicInformation;

@Repository
@RequiredArgsConstructor
public class MemberDeclarationRepositoryImpl implements MemberDeclarationRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<BoReportWaitListDto.Response.ReportInfo> findReportWaitList(Pageable pageable) {
        return jpaQueryFactory
                .select(Projections.fields(BoReportWaitListDto.Response.ReportInfo.class,
                        memberDeclaration.id,
                        ExpressionUtils.as(Expressions.constant(ReportType.MEMBER.name()), "type"),
                        basicInformation.name.as("reporter"),
                        memberDeclaration.reporterId,
                        memberDeclaration.reportedId,
                        memberDeclaration.reason,
                        memberDeclaration.createdAt
                ))
                .from(memberDeclaration)
                .join(member).on(member.id.eq(memberDeclaration.reporterId))
                .leftJoin(basicInformation)
                .on(basicInformation.member.id.eq(memberDeclaration.reporterId))
                .where(memberDeclaration.declarationType.eq(DeclarationType.PENDING))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<BoReportDoneListDto.Response.ReportInfo> findReportDoneList(Pageable pageable) {
        return jpaQueryFactory
                .select(Projections.fields(BoReportDoneListDto.Response.ReportInfo.class,
                        memberDeclaration.id,
                        ExpressionUtils.as(Expressions.constant(ReportType.MEMBER.name()), "type"),
                        basicInformation.name.as("reporter"),
                        memberDeclaration.declarationType,
                        memberDeclaration.createdAt
                ))
                .from(memberDeclaration)
                .join(member).on(member.id.eq(memberDeclaration.reporterId))
                .leftJoin(basicInformation)
                .on(basicInformation.member.id.eq(memberDeclaration.reporterId))
                .where(memberDeclaration.declarationType.ne(DeclarationType.PENDING))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }
}
