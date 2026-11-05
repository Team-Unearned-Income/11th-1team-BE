package org.example.knockin.alarm.repository;

import org.example.knockin.alarm.entity.NotificationAlarm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationAlarmRepository extends JpaRepository<NotificationAlarm, Long> {
}