package org.example.knockin.room.repository.row;

import org.example.knockin.global.util.HasMemberId;

public record MatchingSeekerRegionRow(
        Long memberId,
        String regionName,
        String parentRegionName,
        String grandParentRegionName
) implements HasMemberId {
}
