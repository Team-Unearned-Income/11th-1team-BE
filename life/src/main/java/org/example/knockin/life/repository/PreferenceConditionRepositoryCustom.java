package org.example.knockin.life.repository;


import java.util.List;
import org.example.knockin.life.repository.row.MatchingPreferenceConditionRow;

public interface PreferenceConditionRepositoryCustom {
    List<MatchingPreferenceConditionRow> findConditionRowsByMemberId(Long memberId);

    List<MatchingPreferenceConditionRow> findAllPreferenceConditionByMemberIdIn(List<Long> memberIds);

    List<Long> findLifeInformationIdByMemberId(Long memberId);
}
