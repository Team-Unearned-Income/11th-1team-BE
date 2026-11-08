package org.example.knockin.alarm.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.knockin.meta.dto.BoNoticeDetailDto;
import org.example.knockin.meta.dto.BoNoticeListDto;
import org.example.knockin.meta.dto.NoticeDetailDto;
import org.example.knockin.meta.dto.NoticeListDto;
import org.example.knockin.alarm.entity.Notification;
import org.example.knockin.alarm.repository.NotificationRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl {
    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification saveNotification(Notification notification) {
        return notificationRepository.save(notification);
    }

    public List<BoNoticeListDto.Response.NoticeItem> findBoNotificationList(Pageable pageable) {
        return notificationRepository.findBoNotificationsByIsDeleted(false, pageable);
    }

    public List<NoticeListDto.Response.NoticeItem> findNotificationList(Pageable pageable) {
        return notificationRepository.findNotificationsByIsDeleted(false, pageable);
    }

    public Notification findNotificationById(Long id) {
        return notificationRepository.findByIdAndIsDeleted(id, false);
    }

    public BoNoticeDetailDto.Response findBoNotification(Long id) {
        return BoNoticeDetailDto.Response.builder().notice(notificationRepository.findBoNotificationByIsDeleted(false, id)).build();
    }

    public NoticeDetailDto.Response findNotification(Long id) {
        return NoticeDetailDto.Response.builder().notice(notificationRepository.findNotificationByIsDeleted(false, id)).build();
    }

    public NoticeListDto.Response findNoticeList(Pageable pageable) {
        return NoticeListDto.Response.builder().notices(findNotificationList(pageable)).build();
    }
}
