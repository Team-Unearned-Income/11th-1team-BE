package org.example.knockin.alarm.repository;

import org.example.knockin.alarm.entity.Alarm;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlarmRepository extends JpaRepository<Alarm, Long> {
    List<Alarm> findByMemberId(Long memberId, Pageable pageable);

    Optional<Alarm> findByIdAndMemberId(Long id, Long memberId);

    List<Alarm> findByMemberIdAndIsRead(Long memberId, Boolean isRead);
}
