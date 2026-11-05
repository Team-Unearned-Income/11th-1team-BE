package org.example.knockin.alarm.repository;

import org.example.knockin.alarm.entity.Alarm;
import org.example.knockin.member.entity.Member;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlarmRepository extends JpaRepository<Alarm, Long> {
    List<Alarm> findByMember(Member member, Pageable pageable);

    Optional<Alarm> findByIdAndMember(Long id, Member member);

    List<Alarm> findByMemberAndIsRead(Member member, Boolean isRead);
}