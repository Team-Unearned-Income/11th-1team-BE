package org.example.knockin.alarm.repository;

import org.example.knockin.meta.dto.BoNoticeDetailDto;
import org.example.knockin.meta.dto.BoNoticeListDto;
import org.example.knockin.meta.dto.NoticeDetailDto;
import org.example.knockin.meta.dto.NoticeListDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationRepositoryCustom  {
    List<BoNoticeListDto.Response.NoticeItem> findBoNotificationsByIsDeleted(Boolean isDeleted, Pageable pageable);
    List<NoticeListDto.Response.NoticeItem> findNotificationsByIsDeleted(Boolean isDeleted, Pageable pageable);
    BoNoticeDetailDto.Response.NoticeDetail findBoNotificationByIsDeleted(Boolean isDeleted, Long id);
    NoticeDetailDto.Response.NoticeDetail findNotificationByIsDeleted(Boolean isDeleted, Long id);
}