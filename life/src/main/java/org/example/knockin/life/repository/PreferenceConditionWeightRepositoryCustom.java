package org.example.knockin.life.repository;

import java.util.List;
import org.example.knockin.life.repository.row.MatchingPreferenceConditionWeightRow;

public interface PreferenceConditionWeightRepositoryCustom {
    List<MatchingPreferenceConditionWeightRow> findConditionWeightRowsByMemberId(Long memberId);

    List<MatchingPreferenceConditionWeightRow> findAllPreferenceConditionWeightByMemberIdIn(List<Long> memberIds);
}
