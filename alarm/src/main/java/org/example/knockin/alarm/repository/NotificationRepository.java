package org.example.knockin.alarm.repository;

import org.example.knockin.alarm.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long>, NotificationRepositoryCustom {
    Notification findByIdAndIsDeleted(Long id, Boolean isDeleted);
}