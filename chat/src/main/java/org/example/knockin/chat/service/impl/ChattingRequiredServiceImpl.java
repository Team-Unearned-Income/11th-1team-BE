package org.example.knockin.chat.service.impl;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.knockin.chat.entity.ChattingRequired;
import org.example.knockin.chat.entity.ChattingRequiredStatus;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.RequiredErrorCode;
import org.example.knockin.chat.repository.ChattingRequiredRepository;
import org.example.knockin.chat.repository.row.ChatRequestListRow;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
@RequiredArgsConstructor
public class ChattingRequiredServiceImpl {

    private final ChattingRequiredRepository chattingRequiredRepository;

    public List<Long> findIdsForMembers(List<Long> memberIds) {
        return chattingRequiredRepository.findIdsForMembers(memberIds);
    }

    public List<Long> findRoomIdsByRequestIds(List<Long> requestIds) {
        return chattingRequiredRepository.findRoomIdsByRequestIds(requestIds);
    }

    public List<Long> findScoreIdsByRequestIds(List<Long> requestIds) {
        return chattingRequiredRepository.findScoreIdsByRequestIds(requestIds);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteForMembers(List<Long> memberIds) {
        chattingRequiredRepository.deleteForMembers(memberIds);
    }

    public List<ChatRequestListRow> findAllPendingByRequesteeId(Long requesteeId) {
        return chattingRequiredRepository.findAllPendingByRequesteeId(requesteeId);
    }

    public ChattingRequired findByIdOrThrow(Long requestId) {
        return chattingRequiredRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(RequiredErrorCode.CHATTING_NOT_FOUND));
    }

    public Optional<ChattingRequired> findLatest(Long requesterId, Long requesteeId) {
        return chattingRequiredRepository.findLatest(requesterId, requesteeId);
    }

    public ChattingRequired savePending(Long requesterId, Long requesteeId, @Nullable Long roommateBoardId) {
        return save(requesterId, requesteeId, roommateBoardId, ChattingRequiredStatus.PENDING);
    }

    public ChattingRequired saveAccepted(Long requesterId, Long requesteeId, @Nullable Long roommateBoardId) {
        return save(requesterId, requesteeId, roommateBoardId, ChattingRequiredStatus.ACCEPTED);
    }

    private ChattingRequired save(Long requesterId, Long requesteeId, @Nullable Long roommateBoardId, ChattingRequiredStatus status) {
        ChattingRequired chattingRequired = ChattingRequired.builder()
                .requesterId(requesterId)
                .requesteeId(requesteeId)
                .roommateBoardId(roommateBoardId)
                .status(status)
                .build();

        return chattingRequiredRepository.save(chattingRequired);
    }
}
