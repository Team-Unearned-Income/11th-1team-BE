package org.example.knockin.life.repository;

import org.example.knockin.life.entity.PreferenceConditionWeight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreferenceConditionWeightRepository extends JpaRepository<PreferenceConditionWeight, Long>, PreferenceConditionWeightRepositoryCustom {
    void deleteByMemberId(Long memberId);

    List<PreferenceConditionWeight> findByMemberId(Long memberId);

    List<PreferenceConditionWeight> findAllByMemberId(Long memberId);
}