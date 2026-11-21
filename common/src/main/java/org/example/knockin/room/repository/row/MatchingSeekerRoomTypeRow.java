package org.example.knockin.room.repository.row;

import org.example.knockin.global.util.HasMemberId;

public record MatchingSeekerRoomTypeRow(
        Long memberId,
        String roomTypeName
) implements HasMemberId {
}
