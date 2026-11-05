package org.example.knockin.alarm.repository;

import org.example.knockin.alarm.entity.ChattingRequiredAlarm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChattingRequiredAlarmRepository extends JpaRepository<ChattingRequiredAlarm, Long> {
}