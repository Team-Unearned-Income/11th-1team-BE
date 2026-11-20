package org.example.knockin.room.repository.row;

import org.example.knockin.global.util.HasMemberId;

public record MatchingSeekerProfileRow(
        Long memberId,
        Integer minDeposit,
        Integer maxDeposit,
        Integer minMonthlyRent,
        Integer maxMonthlyRent
) implements HasMemberId {
}