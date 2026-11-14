package org.example.knockin.life.service.impl;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.LifePatternErrorCode;
import org.example.knockin.global.exception.OnBoardErrorCode;
import org.example.knockin.life.dto.ModifyPreferencesLifeStyleDto;
import org.example.knockin.life.entity.LifePattern;
import org.example.knockin.life.entity.LifePatternInformation;
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
import org.example.knockin.member.entity.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PreferenceConditionServiceImpl {
    private final LifeStyleServiceImpl lifeStyleService;
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
    public List<Long> saveAll(Long memberId, List<Long> LifeInformationId) {
        List<LifePatternInformation> lifePatternInformationList = lifeStyleService.findLifePatternInformationByIds(LifeInformationId);

        return lifePatternInformationList.stream()
                .map(lifePatternInformation -> save(memberId, lifePatternInformation))
                .map(PreferenceCondition::getId)
                .toList();
    }

    @Transactional
    public PreferenceCondition save(Long memberId, LifePatternInformation lifePatternInformation) {

        PreferenceCondition preferenceCondition = PreferenceCondition.builder()
                .memberId(memberId)
                .lifePatternInformation(lifePatternInformation)
                .build();

        return preferenceConditionRepository.save(preferenceCondition);
    }

    @Transactional
    public List<PreferenceConditionLog> preferenceConditionLogSaveAll(List<PreferenceConditionLog> preferenceConditionLogList) {
        return preferenceConditionLogRepository.saveAll(preferenceConditionLogList);
    }

    @Transactional
    public List<Long> saveLogsAll(Long memberId, Long degreeId) {
        List<LifePatternInformation> lifePatternInformationList = findPreferenceConditionByMemberId(memberId).stream()
                .map(PreferenceCondition::getLifePatternInformation)
                .toList();

        return saveLogs(memberId, degreeId, lifePatternInformationList);
    }

    @Transactional
    public List<Long> saveLogsAll(Long memberId, Long degreeId, List<Long> lifePatternInformationIds) {
        List<LifePatternInformation> lifePatternInformationList = lifeStyleService.findLifePatternInformationAllById(lifePatternInformationIds);
        return saveLogs(memberId, degreeId, lifePatternInformationList);
    }

    private List<Long> saveLogs(Long memberId, Long degreeId, List<LifePatternInformation> lifePatternInformationList) {
        PreferenceConditionLogDegree degree = preferenceConditionLogDegreeRepository.findById(degreeId)
                .orElseThrow(() -> new BusinessException(LifePatternErrorCode.DEGREE_NOT_FOUND));

        return lifePatternInformationList.stream()
                .map(lifePatternInformation -> saveLog(memberId, degree, lifePatternInformation))
                .map(PreferenceConditionLog::getId)
                .toList();
    }

    private PreferenceConditionLog saveLog(Long memberId, PreferenceConditionLogDegree degree, LifePatternInformation lifePatternInformation) {
        PreferenceConditionLog log = PreferenceConditionLog.builder()
                .memberId(memberId)
                .preferenceConditionLogDegree(degree)
                .lifePatternInformation(lifePatternInformation)
                .build();

        return preferenceConditionLogRepository.save(log);
    }

    @Deprecated
    @Transactional
    public List<PreferenceConditionWeight> preferenceConditionWeightSaveAll(List<PreferenceConditionWeight> preferenceConditionWeightList) {
        return preferenceConditionWeightRepository.saveAll(preferenceConditionWeightList);
    }

    @Transactional
    public List<Long> savePreferenceConditionWeights(Long memberId, List<Long> lifePatternIds) {
        List<LifePattern> lifePatterns = lifeStyleService.findAllById(lifePatternIds);
        return lifePatterns.stream()
                .map(lifePattern -> savePreferenceConditionWeight(memberId, lifePattern))
                .toList();
    }

    private Long savePreferenceConditionWeight(Long memberId, LifePattern lifePattern) {
        PreferenceConditionWeight conditionWeight = PreferenceConditionWeight.builder()
                .memberId(memberId)
                .lifePattern(lifePattern)
                .build();

        return preferenceConditionWeightRepository.save(conditionWeight).getId();
    }

    @Deprecated
    @Transactional
    public List<PreferenceConditionWeightLog> preferenceConditionWeightLogSaveAll(List<PreferenceConditionWeightLog> preferenceConditionWeightLogList) {
        return preferenceConditionWeightLogRepository.saveAll(preferenceConditionWeightLogList);
    }

    public List<Long> savePreferenceConditionWeightLogs(Long memberId, List<Long> lifePatternIds, Long degreeId) {
        List<LifePattern> lifePatterns = lifeStyleService.findAllById(lifePatternIds);
        PreferenceConditionWeightLogDegree degree = preferenceConditionWeightLogDegreeRepository.findById(degreeId)
                .orElseThrow(() -> new BusinessException(LifePatternErrorCode.DEGREE_NOT_FOUND));

        return lifePatterns.stream()
                .map(lifePattern -> savePreferenceConditionWeightLog(memberId, lifePattern, degree))
                .map(PreferenceConditionWeightLog::getId)
                .toList();
    }

    private PreferenceConditionWeightLog savePreferenceConditionWeightLog(Long memberId, LifePattern lifePattern, PreferenceConditionWeightLogDegree degree) {
        PreferenceConditionWeightLog preferenceConditionWeightLog = PreferenceConditionWeightLog.builder()
                .memberId(memberId)
                .lifePattern(lifePattern)
                .preferenceConditionWeightLogDegree(degree)
                .build();
        return preferenceConditionWeightLogRepository.save(preferenceConditionWeightLog);
    }

    @Transactional
    public void modifyPreferenceConditionWeightLog(Long memberId, Long degreeId) {
        List<PreferenceConditionWeight> preferenceConditionWeightList = findPreferenceConditionWeightByMemberId(memberId);
        PreferenceConditionWeightLogDegree preferenceConditionWeightLogDegree = preferenceConditionWeightLogDegreeRepository.findById(degreeId)
                .orElseThrow(() -> new BusinessException(LifePatternErrorCode.DEGREE_NOT_FOUND));

        List<PreferenceConditionWeightLog> logList = preferenceConditionWeightList.stream()
                .map(patternWeight -> PreferenceConditionWeightLog.builder()
                        .memberId(memberId)
                        .lifePattern(patternWeight.getLifePattern())
                        .preferenceConditionWeightLogDegree(preferenceConditionWeightLogDegree)
                        .build())
                .toList();

        if (!logList.isEmpty()) {
            preferenceConditionWeightLogRepository.saveAll(logList);
        }
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

    public List<MatchingPreferenceConditionRow> findConditionRowsByMemberId(Long memberId) {
        return preferenceConditionRepository.findConditionRowsByMemberId(memberId);
    }

    public List<MatchingPreferenceConditionWeightRow> findConditionWeightRowsByMemberId(Long memberId) {
        return preferenceConditionWeightRepository.findConditionWeightRowsByMemberId(memberId);
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

    public Long findNextLogDegree(Long memberId) {
        Long maxDegree = preferenceConditionLogDegreeRepository.findMaxPreferenceConditionLogDegree(memberId).orElse(null);
        return maxDegree != null ? maxDegree + 1 : 1L;
    }

    @Transactional
    public PreferenceConditionLogDegree preferenceConditionLogDegreeSave(Long degree) {
        return preferenceConditionLogDegreeRepository.save(PreferenceConditionLogDegree.builder().degree(degree).build());
    }

    @Transactional
    public Long saveLogDegree(Long degree) {
        return preferenceConditionLogDegreeRepository.save(PreferenceConditionLogDegree.builder().degree(degree).build()).getId();
    }

    public Long findNextWeightLogDegreeNumber(Long memberId) {
        Long maxDegree = preferenceConditionWeightLogDegreeRepository.findMaxPreferenceConditionWeightLogDegree(memberId).orElse(null);
        return maxDegree != null ? maxDegree + 1 : 1L;
    }

    @Transactional
    public PreferenceConditionWeightLogDegree preferenceConditionWeightLogDegreeSave(Long degree) {
        return preferenceConditionWeightLogDegreeRepository.save(
                PreferenceConditionWeightLogDegree.builder().degree(degree).build());
    }

    @Transactional
    public Long savePreferenceConditionWeightLogDegree(Long degree) {
        return preferenceConditionWeightLogDegreeSave(degree).getId();
    }

    @Transactional
    public void modifyPreferenceLifeStyle(ModifyPreferencesLifeStyleDto.Request request, Member member) {
        Map<Long, LifePatternInformation> newInfoMap = request.getLifestyles().stream().collect(
                Collectors.toMap(ModifyPreferencesLifeStyleDto.Request.LifeStyleInfo::getId, item -> lifeStyleService.findLifePatternInformationById(item.getLifestyleId())));
        findPreferenceConditionByMemberId(member.getId()).forEach(item -> {
            request.getLifestyles().forEach(data -> {
                if(Objects.equals(data.getId(), item.getId())) {
                    LifePatternInformation newInfo = newInfoMap.get(data.getId());

                    if(!Objects.equals(newInfo.getLifePattern().getId(), item.getLifePatternInformation().getLifePattern().getId())) {
                        throw new BusinessException(OnBoardErrorCode.ONBOARD_LIFE_STYLE_VAILDATION_FAIL);
                    }

                    item.modifyLifePatternInformation(newInfoMap.get(item.getId()));
                }
            });
        });
    }
}
