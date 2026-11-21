package org.example.knockin.global.util;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface HasMemberId {
    Long memberId();

    static <T extends HasMemberId> Map<Long, T> toMapByMemberId(List<T> rows) {
        return rows.stream().collect(Collectors.toMap(
                HasMemberId::memberId,
                Function.identity()
        ));
    }

    static <T extends HasMemberId> Map<Long, List<T>> groupingByMemberId(List<T> rows) {
        return rows.stream()
                .collect(Collectors.groupingBy(HasMemberId::memberId));
    }

    static <T extends HasMemberId, R> Map<Long, List<R>> groupingByMemberId(List<T> rows, Function<T, R> mapper) {
        return rows.stream()
                .collect(Collectors.groupingBy(
                        HasMemberId::memberId,
                        Collectors.mapping(mapper, Collectors.toList())
                ));
    }
}
