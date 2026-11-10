package org.example.knockin.chat.repository;

import java.util.List;
import java.util.Optional;
import org.example.knockin.chat.entity.ChattingRequired;
import org.example.knockin.chat.repository.row.ChatRequestListRow;

public interface ChattingRequiredRepositoryCustom {
    List<Long> findIdsForMembers(List<Long> memberIds);

    List<Long> findRoomIdsByRequestIds(List<Long> requestIds);

    List<Long> findScoreIdsByRequestIds(List<Long> requestIds);

    void deleteForMembers(List<Long> memberIds);

    boolean existsBetweenMembers(Long memberAId, Long memberBId);

    Optional<ChattingRequired> findLatest(Long memberAId, Long memberBId);

    List<ChatRequestListRow> findAllPendingByRequesteeId(Long requesteeId);
}
