package org.example.knockin.room.repository.row;

import org.example.knockin.global.util.HasMemberId;

public record MatchingOfferProfileRow(
        Long memberId,
        Integer deposit,
        Integer monthlyRent,
        String regionName,
        String parentRegionName,
        String grandParentRegionName,
        String roomTypeName
) implements HasMemberId {
}
