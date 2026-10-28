package org.example.knockin.life.repository;


import java.util.Optional;

public interface MemberLifePatternLogDegreeRepositoryCustom {
    Optional<Long> findMaxmemberLifePatternLogDegree(Long memberId);
}