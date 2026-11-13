package org.example.knockin.life.repository;

import java.util.List;
import org.example.knockin.life.repository.row.MatchingLifestyleRow;

public interface MemberLifePatternRepositoryCustom {
    List<MatchingLifestyleRow> findLifestyleRowsByMemberId(Long memberId);
    boolean isExsitLifeStyle(Long memberId);
    List<MatchingLifestyleRow> findAllLifestyleByMemberIdIn(List<Long> memberIds);
}