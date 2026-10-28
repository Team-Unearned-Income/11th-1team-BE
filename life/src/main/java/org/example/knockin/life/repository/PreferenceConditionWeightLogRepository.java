package org.example.knockin.life.repository;

import org.example.knockin.life.entity.PreferenceConditionWeightLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreferenceConditionWeightLogRepository extends JpaRepository<PreferenceConditionWeightLog, Long>, PreferenceConditionWeightLogRepositoryCustom {
    void deleteByMemberId(Long memberId);
}