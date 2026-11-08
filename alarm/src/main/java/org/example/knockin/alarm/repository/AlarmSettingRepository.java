package org.example.knockin.alarm.repository;

import org.example.knockin.alarm.entity.AlarmSetting;
import org.example.knockin.alarm.entity.AlarmSettingType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlarmSettingRepository extends JpaRepository<AlarmSetting, Long> {
    List<AlarmSetting> findByMemberId(Long memberId);

    AlarmSetting findByIdAndMemberId(Long id, Long memberId);

    boolean existsByMemberIdAndAlarmSettingTypeAndIsEnabledTrue(Long memberId, AlarmSettingType alarmSettingType);
}
