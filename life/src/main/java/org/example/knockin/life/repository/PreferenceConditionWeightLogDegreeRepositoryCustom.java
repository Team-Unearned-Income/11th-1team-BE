package org.example.knockin.life.repository;


import java.util.Optional;

public interface PreferenceConditionWeightLogDegreeRepositoryCustom {
    Optional<Long> findMaxPreferenceConditionWeightLogDegree(Long memberId);
}
