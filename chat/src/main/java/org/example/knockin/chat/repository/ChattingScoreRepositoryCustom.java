package org.example.knockin.chat.repository;

import java.util.Optional;
import java.util.List;
import org.example.knockin.chat.entity.ChattingScore;

public interface ChattingScoreRepositoryCustom {
    Optional<ChattingScore> findByChattingRoomIdAndMemberId(Long chattingRoomId, Long memberId);

    List<ChattingScore> findByChattingRoomId(Long chattingRoomId);

    Optional<ChattingScore> findOneByChattingRequiredIdAndMemberId(Long chattingRequiredId, Long memberId);
}