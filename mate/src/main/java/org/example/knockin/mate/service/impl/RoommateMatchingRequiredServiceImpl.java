package org.example.knockin.mate.service.impl;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.knockin.mate.dto.RoommateRequestDto.RoommateMatchingRequiredInfo;
import org.example.knockin.member.entity.Member;
import org.example.knockin.mate.entity.RoommateMatchingRequired;
import org.example.knockin.mate.entity.RoommateRequiredStatus;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.RequiredErrorCode;
import org.example.knockin.mate.repository.RoommateMatchingRequiredRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
@RequiredArgsConstructor
public class RoommateMatchingRequiredServiceImpl {

    private final RoommateMatchingRequiredRepository roommateMatchingRequiredRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteForMembers(List<Long> memberIds, List<Long> chatRoomIds, List<Long> chatScoreIds) {
        roommateMatchingRequiredRepository.deleteForMembers(memberIds, chatRoomIds, chatScoreIds);
    }

    public List<RoommateMatchingRequiredInfo> findRequiredDto(Long chattingRoomId) {
        return roommateMatchingRequiredRepository.findRequiredDto(chattingRoomId);
    }

    public Optional<RoommateMatchingRequired> findLatest(Long chatRoomId) {
        return roommateMatchingRequiredRepository.findLatest(chatRoomId);
    }

    public RoommateMatchingRequired findByIdOrThrow(Long requestId) {
        return roommateMatchingRequiredRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(RequiredErrorCode.ROOMMATE_NOT_FOUND));
    }

    public RoommateMatchingRequired savePending(Member requester, Member requestee, Long chattingRoomId) {
        RoommateMatchingRequired roommateMatchingRequired = RoommateMatchingRequired.builder()
                .requester(requester)
                .requestee(requestee)
                .chattingRoomId(chattingRoomId)
                .status(RoommateRequiredStatus.PENDING)
                .build();

        return roommateMatchingRequiredRepository.save(roommateMatchingRequired);
    }

    public Page<RoommateMatchingRequired> findByRequesterIdAndRequesteeId(Long requesterId, Long requesteeId, Pageable pageable) {
        return roommateMatchingRequiredRepository.findByRequesterIdAndRequesteeId(requesterId, requesteeId, pageable);
    }

    public Page<RoommateMatchingRequired> findMyRequiredList(Long memberId,  Pageable pageable) {
        return roommateMatchingRequiredRepository.findMyRequiredList(memberId, pageable);
    }
}
