package org.example.knockin.alarm.repository;

import org.example.knockin.alarm.entity.RoommateCalendarAlarm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoommateCalendarAlarmRepository extends JpaRepository<RoommateCalendarAlarm, Long> {
}