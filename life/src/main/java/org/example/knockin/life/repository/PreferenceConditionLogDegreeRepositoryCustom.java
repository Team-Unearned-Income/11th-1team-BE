package org.example.knockin.life.repository;


import java.util.Optional;

public interface PreferenceConditionLogDegreeRepositoryCustom {
    Optional<Long> findMaxPreferenceConditionLogDegree(Long memberId);
}
