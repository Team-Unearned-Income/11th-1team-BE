package org.example.knockin.alarm.repository;

import org.example.knockin.alarm.entity.AlarmSetting;
import org.example.knockin.alarm.entity.AlarmSettingType;
import org.example.knockin.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlarmSettingRepository extends JpaRepository<AlarmSetting, Long> {
    List<AlarmSetting> findByMember(Member member);

    AlarmSetting findByIdAndMember(Long id, Member member);

    boolean existsByMemberAndAlarmSettingTypeAndIsEnabledTrue(Member member, AlarmSettingType alarmSettingType);
}