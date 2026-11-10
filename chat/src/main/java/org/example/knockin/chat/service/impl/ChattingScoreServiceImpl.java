package org.example.knockin.chat.service.impl;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.knockin.chat.entity.ChattingScore;
import org.example.knockin.chat.repository.ChattingScoreRepository;
import org.example.knockin.chat.entity.ChattingRequired;
import org.example.knockin.life.entity.MemberLifePatternLog;
import org.example.knockin.life.entity.MemberLifePatternLogDegree;
import org.example.knockin.life.entity.PreferenceConditionLog;
import org.example.knockin.life.entity.PreferenceConditionLogDegree;
import org.example.knockin.life.entity.PreferenceConditionWeightLog;
import org.example.knockin.life.entity.PreferenceConditionWeightLogDegree;
import org.example.knockin.life.repository.MemberLifePatternLogRepository;
import org.example.knockin.life.repository.PreferenceConditionLogRepository;
import org.example.knockin.life.repository.PreferenceConditionWeightLogRepository;
import org.example.knockin.util.service.RoommateScoreService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChattingScoreServiceImpl {

    private final ChattingScoreRepository chattingScoreRepository;
    private final RoommateScoreService javaRoommateScoreV2Service;
    private final MemberLifePatternLogRepository memberLifePatternLogRepository;
    private final PreferenceConditionLogRepository preferenceConditionLogRepository;
    private final PreferenceConditionWeightLogRepository preferenceConditionWeightLogRepository;

    @Transactional
    public List<ChattingScore> createAndSaveScores(ChattingRequired chattingRequired) {
        Long requesterId = chattingRequired.getRequesterId();
        Long requesteeId = chattingRequired.getRequesteeId();

        ChattingScore requesterChattingScore = createChattingScore(requesterId, requesteeId, chattingRequired);
        ChattingScore requesteeChattingScore = createChattingScore(requesteeId, requesterId, chattingRequired);

        return chattingScoreRepository.saveAll(List.of(requesterChattingScore, requesteeChattingScore));
    }

    private ChattingScore createChattingScore(Long requesterId, Long requesteeId, ChattingRequired chattingRequired) {
        int score = javaRoommateScoreV2Service.calculateScore(requesterId, requesteeId).getTotalScore();
        List<MemberLifePatternLog> memberLifePatternLogList = memberLifePatternLogRepository.findLatestLogsWithFetchByMemberId(requesterId);
        MemberLifePatternLogDegree memberLifePatternLogDegree = memberLifePatternLogList.isEmpty() ? null : memberLifePatternLogList.getFirst().getMemberLifePatternLogDegree();
        List<PreferenceConditionLog> preferenceConditionLogList = preferenceConditionLogRepository.findLatestLogsWithFetchByMemberId(requesterId);
        PreferenceConditionLogDegree preferenceConditionLogDegree = preferenceConditionLogList.isEmpty() ? null : preferenceConditionLogList.getFirst().getPreferenceConditionLogDegree();
        List<PreferenceConditionWeightLog> preferenceConditionWeightLogList = preferenceConditionWeightLogRepository.findLatestLogsWithFetchByMemberId(requesterId);
        PreferenceConditionWeightLogDegree preferenceConditionWeightLogDegree = preferenceConditionWeightLogList.isEmpty() ? null : preferenceConditionWeightLogList.getFirst().getPreferenceConditionWeightLogDegree();

        return ChattingScore.builder().chattingRequired(chattingRequired)
                .memberLifePatternLogDegreeId(memberLifePatternLogDegree == null ? null : memberLifePatternLogDegree.getId())
                .preferenceConditionLogDegreeId(preferenceConditionLogDegree == null ? null : preferenceConditionLogDegree.getId())
                .preferenceConditionWeightLogDegreeId(preferenceConditionWeightLogDegree == null ? null : preferenceConditionWeightLogDegree.getId())
                .score(score).build();
    }

    public Optional<ChattingScore> findByChattingRoomIdAndMemberId(Long chattingRoomId, Long memberId) {
        return chattingScoreRepository.findByChattingRoomIdAndMemberId(chattingRoomId, memberId);
    }

    public Optional<ChattingScore> findById(Long id) {
        return id == null ? Optional.empty() : chattingScoreRepository.findById(id);
    }

    public List<ChattingScore> findByChattingRoomId(Long chattingRoomId) {
        return chattingScoreRepository.findByChattingRoomId(chattingRoomId);
    }

    public List<ChattingScore> saveAll(List<ChattingScore> chattingScores) {
        return chattingScoreRepository.saveAll(chattingScores);
    }

    public Optional<ChattingScore> findByChattingRequiredIdAndMemberId(Long chattingRequiredId, Long memberId) {
        return chattingScoreRepository.findOneByChattingRequiredIdAndMemberId(chattingRequiredId, memberId);
    }

    public List<ChattingScore> findByChattingRequiredId(Long chattingRequiredId) {
        return chattingScoreRepository.findByChattingRequiredId(chattingRequiredId);
    }
}
