package org.example.knockin.alarm.repository.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.knockin.alarm.repository.NotificationRepositoryCustom;
import org.example.knockin.meta.dto.BoNoticeDetailDto;
import org.example.knockin.meta.dto.BoNoticeListDto;
import org.example.knockin.meta.dto.NoticeDetailDto;
import org.example.knockin.meta.dto.NoticeListDto;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.example.knockin.alarm.entity.QNotification.notification;
import static org.example.knockin.member.entity.QMember.member;
import static org.example.knockin.member.entity.QBasicInformation.basicInformation;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<BoNoticeListDto.Response.NoticeItem> findBoNotificationsByIsDeleted(Boolean isDeleted, Pageable pageable) {
        return jpaQueryFactory
                .select(Projections.fields(
                        BoNoticeListDto.Response.NoticeItem.class,
                        notification.id,
                        notification.title,
                        notification.createdAt.as("createAt"),
                        basicInformation.name.as("writer")
                ))
                .from(notification)
                .join(notification.member, member)
                .leftJoin(basicInformation).on(basicInformation.member.eq(member))
                .where(notification.isDeleted.eq(isDeleted))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<NoticeListDto.Response.NoticeItem> findNotificationsByIsDeleted(Boolean isDeleted, Pageable pageable) {
        return jpaQueryFactory
                .select(Projections.fields(
                        NoticeListDto.Response.NoticeItem.class,
                        notification.id,
                        notification.title,
                        notification.createdAt.as("createAt"),
                        basicInformation.name.as("writer")
                ))
                .from(notification)
                .join(notification.member, member)
                .leftJoin(basicInformation).on(basicInformation.member.eq(member))
                .where(notification.isDeleted.eq(isDeleted))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public BoNoticeDetailDto.Response.NoticeDetail findBoNotificationByIsDeleted(Boolean isDeleted, Long id) {
        return jpaQueryFactory
                .select(Projections.fields(
                        BoNoticeDetailDto.Response.NoticeDetail.class,
                        notification.id,
                        notification.title,
                        notification.contents,
                        notification.createdAt.as("createAt"),
                        basicInformation.name.as("writer")
                ))
                .from(notification)
                .join(notification.member, member)
                .leftJoin(basicInformation).on(basicInformation.member.eq(member))
                .where(notification.isDeleted.eq(isDeleted), notification.id.eq(id))
                .fetchOne();
    }

    @Override
    public NoticeDetailDto.Response.NoticeDetail findNotificationByIsDeleted(Boolean isDeleted, Long id) {
        return jpaQueryFactory
                .select(Projections.fields(
                        NoticeDetailDto.Response.NoticeDetail.class,
                        notification.id,
                        notification.title,
                        notification.contents,
                        notification.createdAt.as("createAt"),
                        basicInformation.name.as("writer")
                ))
                .from(notification)
                .join(notification.member, member)
                .leftJoin(basicInformation).on(basicInformation.member.eq(member))
                .where(notification.isDeleted.eq(isDeleted), notification.id.eq(id))
                .fetchOne();
    }
}