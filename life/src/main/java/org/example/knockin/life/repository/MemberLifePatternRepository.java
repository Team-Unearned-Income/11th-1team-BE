package org.example.knockin.life.repository;

import org.example.knockin.life.entity.MemberLifePattern;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MemberLifePatternRepository extends JpaRepository<MemberLifePattern, Long>, MemberLifePatternRepositoryCustom {
    List<MemberLifePattern> findByMemberId(Long memberId);

    List<MemberLifePattern> findAllByMemberIdIn(Collection<Long> memberIds);
}
