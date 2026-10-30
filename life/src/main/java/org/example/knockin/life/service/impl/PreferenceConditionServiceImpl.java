package org.example.knockin.life.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.knockin.board.dto.BoardDetailDto.Response.Condition;
import org.example.knockin.board.dto.BoardDetailDto.Response.ConditionWeight;
import org.example.knockin.life.entity.PreferenceCondition;
import org.example.knockin.life.entity.PreferenceConditionLog;
import org.example.knockin.life.entity.PreferenceConditionLogDegree;
import org.example.knockin.life.entity.PreferenceConditionWeight;
import org.example.knockin.life.entity.PreferenceConditionWeightLog;
import org.example.knockin.life.entity.PreferenceConditionWeightLogDegree;
import org.example.knockin.life.repository.PreferenceConditionLogDegreeRepository;
import org.example.knockin.life.repository.PreferenceConditionLogRepository;
import org.example.knockin.life.repository.PreferenceConditionRepository;
import org.example.knockin.life.repository.PreferenceConditionWeightLogDegreeRepository;
import org.example.knockin.life.repository.PreferenceConditionWeightLogRepository;
import org.example.knockin.life.repository.PreferenceConditionWeightRepository;
import org.example.knockin.life.repository.row.MatchingPreferenceConditionRow;
import org.example.knockin.life.repository.row.MatchingPreferenceConditionWeightRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PreferenceConditionServiceImpl {
    private final PreferenceConditionRepository preferenceConditionRepository;
    private final PreferenceConditionLogRepository preferenceConditionLogRepository;
    private final PreferenceConditionLogDegreeRepository preferenceConditionLogDegreeRepository;
    private final PreferenceConditionWeightRepository preferenceConditionWeightRepository;
    private final PreferenceConditionWeightLogRepository preferenceConditionWeightLogRepository;
    private final PreferenceConditionWeightLogDegreeRepository preferenceConditionWeightLogDegreeRepository;

    @Transactional
    public List<PreferenceCondition> preferenceConditionSaveAll(List<PreferenceCondition> preferenceConditionList) {
        return preferenceConditionRepository.saveAll(preferenceConditionList);
    }

    @Transactional
    public List<PreferenceConditionLog> preferenceConditionLogSaveAll(List<PreferenceConditionLog> preferenceConditionLogList) {
        return preferenceConditionLogRepository.saveAll(preferenceConditionLogList);
    }

    @Transactional
    public List<PreferenceConditionWeight> preferenceConditionWeightSaveAll(List<PreferenceConditionWeight> preferenceConditionWeightList) {
        return preferenceConditionWeightRepository.saveAll(preferenceConditionWeightList);
    }

    @Transactional
    public List<PreferenceConditionWeightLog> preferenceConditionWeightLogSaveAll(List<PreferenceConditionWeightLog> preferenceConditionWeightLogList) {
        return preferenceConditionWeightLogRepository.saveAll(preferenceConditionWeightLogList);
    }

    public List<PreferenceCondition> findPreferenceConditionByMemberId(Long memberId) {
        return preferenceConditionRepository.findByMemberId(memberId);
    }

    public List<PreferenceConditionWeight> findPreferenceConditionWeightByMemberId(Long memberId) {
        return preferenceConditionWeightRepository.findByMemberId(memberId);
    }

    public List<MatchingPreferenceConditionRow> findRowByMemberIdsIn(List<Long> memberIds) {
        return preferenceConditionRepository.findAllPreferenceConditionByMemberIdIn(memberIds);
    }

    public List<MatchingPreferenceConditionWeightRow> findWeightRowByMemberIdsIn(List<Long> memberIds) {
        return preferenceConditionWeightRepository.findAllPreferenceConditionWeightByMemberIdIn(memberIds);
    }

    public List<Condition> findAllConditionByMemberId(Long memberId) {
        return preferenceConditionRepository.getConditionDtoByMemberId(memberId);
    }

    public List<ConditionWeight> findAllConditionWeightByMemberId(Long memberId) {
        return preferenceConditionWeightRepository.getConditionWeightDtoByMemberId(memberId);
    }

    @Transactional
    public void deletePreferenceConditionWeightByMemberId(Long memberId) {
        preferenceConditionWeightRepository.deleteByMemberId(memberId);
        preferenceConditionWeightRepository.flush();
    }

    @Transactional
    public void deletePreferenceConditionByMemberId(Long memberId) {
        List<PreferenceCondition> existing = preferenceConditionRepository.findByMemberId(memberId);
        if (!existing.isEmpty()) {
            preferenceConditionRepository.deleteAll(existing);
        }
    }

    public Long findMaxPreferenceConditionLogDegree(Long memberId) {
        return preferenceConditionLogDegreeRepository.findMaxPreferenceConditionLogDegree(memberId).orElse(null);
    }

    @Transactional
    public PreferenceConditionLogDegree preferenceConditionLogDegreeSave(Long degree) {
        return preferenceConditionLogDegreeRepository.save(PreferenceConditionLogDegree.builder().degree(degree).build());
    }

    public Long findMaxPreferenceConditionWeightLogDegree(Long memberId) {
        return preferenceConditionWeightLogDegreeRepository.findMaxPreferenceConditionWeightLogDegree(memberId).orElse(null);
    }

    @Transactional
    public PreferenceConditionWeightLogDegree preferenceConditionWeightLogDegreeSave(Long degree) {
        return preferenceConditionWeightLogDegreeRepository.save(
                PreferenceConditionWeightLogDegree.builder().degree(degree).build());
    }
}
