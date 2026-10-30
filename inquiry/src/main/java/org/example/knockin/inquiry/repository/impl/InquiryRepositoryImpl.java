package org.example.knockin.inquiry.repository.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.knockin.inquiry.dto.BoInquiryDetailDto;
import org.example.knockin.inquiry.dto.BoInquiryListDto;
import org.example.knockin.inquiry.dto.InquiryDetailDto;
import org.example.knockin.inquiry.dto.InquiryListDto;
import org.example.knockin.inquiry.repository.InquiryRepositoryCustom;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.example.knockin.inquiry.entity.QInquiry.inquiry;
import static org.example.knockin.member.entity.QBasicInformation.basicInformation;
import static org.example.knockin.inquiry.entity.QInquiryComment.inquiryComment;
import static com.querydsl.core.group.GroupBy.groupBy;
import static com.querydsl.core.group.GroupBy.list;

@Repository
@RequiredArgsConstructor
public class InquiryRepositoryImpl implements InquiryRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<InquiryListDto.Response.InquiryItem> findMyInquiryList(Boolean isDeleted, Long memberId, Pageable pageable) {
        return jpaQueryFactory.select(Projections.fields(InquiryListDto.Response.InquiryItem.class,
                inquiry.id,
                inquiry.title,
                inquiry.createdAt,
                inquiry.inquiryCategory.title.as("type"), basicInformation.name.as("writer"),
                new CaseBuilder().when(inquiryComment.id.isNull()).then("답변 대기중").otherwise("답변 완료").as("status")
                )).from(inquiry).leftJoin(inquiry.inquiryCategory)
                .leftJoin(inquiryComment).on(inquiryComment.inquiry.eq(inquiry))
                .leftJoin(basicInformation).on(basicInformation.member.id.eq(inquiry.memberId))
                .where(inquiry.isDeleted.eq(isDeleted), inquiry.memberId.eq(memberId))
                .offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();
    }

    @Override
    public InquiryDetailDto.Response.InquiryDetail findMyInquiry(Boolean isDeleted, Long memberId, Long inquiryId) {
        return jpaQueryFactory
                .from(inquiry)
                .leftJoin(inquiry.inquiryCategory)
                .leftJoin(inquiryComment).on(inquiryComment.inquiry.eq(inquiry))
                .leftJoin(basicInformation).on(basicInformation.member.id.eq(inquiry.memberId))
                .where(inquiry.id.eq(inquiryId), inquiry.isDeleted.eq(isDeleted), inquiry.memberId.eq(memberId))
                .transform(groupBy(inquiry.id).list(
                        Projections.fields(InquiryDetailDto.Response.InquiryDetail.class,
                                inquiry.id,
                                inquiry.title,
                                inquiry.contents,
                                basicInformation.name.as("writer"),
                                new CaseBuilder().when(inquiryComment.id.isNull()).then("답변 대기중").otherwise("답변 완료").as("status"),
                                inquiry.createdAt.as("createAt"),
                                inquiry.inquiryCategory.title.as("type"),
                                list(Projections.fields(InquiryDetailDto.Response.InquiryDetail.Reply.class,
                                        inquiryComment.id,
                                        inquiryComment.contents,
                                        inquiryComment.createdAt.as("replyCreateAt"))).as("reply"))
                )).stream().findFirst().orElse(null);
    }

    @Override
    public List<BoInquiryListDto.Response.InquiryItem> findBackOfficeInquirieList(Pageable pageable, BoInquiryListDto.Request request) {
        return jpaQueryFactory.select(Projections.fields(BoInquiryListDto.Response.InquiryItem.class,
                inquiry.id,
                inquiry.title,
                inquiry.createdAt.as("createAt"),
                inquiry.inquiryCategory.title.as("type"), basicInformation.name.as("writer"),
                new CaseBuilder().when(inquiryComment.id.isNull()).then("답변 대기중").otherwise("답변 완료").as("status")
                )).from(inquiry).leftJoin(inquiry.inquiryCategory)
                .leftJoin(inquiryComment).on(inquiryComment.inquiry.eq(inquiry))
                .leftJoin(basicInformation).on(basicInformation.member.id.eq(inquiry.memberId))
                .where(inquiry.isDeleted.eq(false), searchKeyword(request.getSearchKeyword()), searchState(request.getIsReply()), searchType(request.getCategoryId()))
                .offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();
    }

    @Override
    public BoInquiryDetailDto.Response.InquiryDetail findBackOfficeInquirie(Long id) {
        return jpaQueryFactory.select(Projections.fields(BoInquiryDetailDto.Response.InquiryDetail.class,
                        inquiry.id,
                        inquiry.title,
                        inquiry.contents,
                        inquiry.createdAt.as("createAt"),
                        inquiry.inquiryCategory.title.as("type"), basicInformation.name.as("writer"),
                        new CaseBuilder().when(inquiryComment.id.isNull()).then("답변 대기중").otherwise("답변 완료").as("status")
                )).from(inquiry).leftJoin(inquiry.inquiryCategory)
                .leftJoin(inquiryComment).on(inquiryComment.inquiry.eq(inquiry))
                .leftJoin(basicInformation).on(basicInformation.member.id.eq(inquiry.memberId))
                .where(inquiry.id.eq(id), inquiry.isDeleted.eq(false)).fetchOne();
    }

    @Override
    public List<BoInquiryDetailDto.Response.InquiryDetail.Reply> findBackOfficeInquirieReply(Long id) {
        return jpaQueryFactory.select(Projections.fields(
                        BoInquiryDetailDto.Response.InquiryDetail.Reply.class,
                        inquiryComment.id,
                        inquiryComment.contents,
                        basicInformation.name.as("writer"),
                        inquiryComment.createdAt.as("createAt")
                ))
                .from(inquiryComment)
                .join(inquiryComment.inquiry, inquiry)
                .join(basicInformation).on(basicInformation.member.id.eq(inquiryComment.memberId))
                .where(inquiry.id.eq(id))
                .fetch();
    }

    private BooleanExpression searchKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return inquiry.title.contains(keyword).or(basicInformation.name.contains(keyword));
    }

    private BooleanExpression searchState(Boolean isReplied) {
        if (isReplied == null) return null;
        return isReplied ? inquiryComment.id.isNotNull() : inquiryComment.id.isNull();
    }

    private BooleanExpression searchType(Long typeId) {
        return typeId != null ? inquiry.inquiryCategory.id.eq(typeId) : null;
    }
}