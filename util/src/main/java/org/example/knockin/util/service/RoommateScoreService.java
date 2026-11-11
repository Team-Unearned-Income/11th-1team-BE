package org.example.knockin.util.service;

import java.util.List;
import java.util.Map;
import org.example.knockin.util.dto.Compatibility;

public interface RoommateScoreService {
    Map<Long, Compatibility> calculateScores(Long requesterId, List<Long> targetMemberIds);

    Map<Long, Integer> calculateSimpleScores(Long requesterId, List<Long> targetMemberIds);

    Compatibility calculateScore(Long requesterId, Long targetMemberId);

    Integer calculateSimpleScore(Long requesterId, Long targetMemberId);
}
