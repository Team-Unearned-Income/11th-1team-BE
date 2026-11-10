package org.example.knockin.util.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.example.knockin.util.dto.Compatibility;
import org.example.knockin.life.entity.LifePatternInformation;
import org.example.knockin.life.entity.LifePatternType;
import org.example.knockin.life.entity.MemberLifePatternLog;
import org.example.knockin.life.entity.MemberLifePatternLogDegree;
import org.example.knockin.life.entity.PreferenceConditionLog;
import org.example.knockin.life.entity.PreferenceConditionWeightLog;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.LifePatternErrorCode;
import org.example.knockin.life.repository.LifePatternInformationRepository;
import org.example.knockin.life.repository.MemberLifePatternLogRepository;
import org.example.knockin.life.repository.MemberLifePatternRepository;
import org.example.knockin.life.repository.PreferenceConditionLogRepository;
import org.example.knockin.life.repository.PreferenceConditionRepository;
import org.example.knockin.life.repository.PreferenceConditionWeightLogRepository;
import org.example.knockin.life.repository.PreferenceConditionWeightRepository;
import org.example.knockin.life.repository.row.LifePatternInformationValueRow;
import org.example.knockin.life.repository.row.MatchingLifestyleRow;
import org.example.knockin.life.repository.row.MatchingPreferenceConditionRow;
import org.example.knockin.life.repository.row.MatchingPreferenceConditionWeightRow;
import org.example.knockin.util.service.RoommateScoreService;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JavaRoommateScoreService implements RoommateScoreService {
    private final MemberLifePatternRepository memberLifePatternRepository;
    private final PreferenceConditionRepository preferenceConditionRepository;
    private final PreferenceConditionWeightRepository preferenceConditionWeightRepository;
    private final LifePatternInformationRepository lifePatternInformationRepository;
    private final RoommateScorePolicy scorePolicy;
    private final MemberLifePatternLogRepository memberLifePatternLogRepository;
    private final PreferenceConditionLogRepository preferenceConditionLogRepository;
    private final PreferenceConditionWeightLogRepository preferenceConditionWeightLogRepository;

    @Override
    public Map<Long, Compatibility> calculateScores(Long requesterId, List<Long> targetMemberIds) {
        if (requesterId == null || targetMemberIds == null || targetMemberIds.isEmpty()) return Map.of();

        List<Long> memberIds = includeRequester(targetMemberIds, requesterId);
        List<MatchingLifestyleRow> lifestyleRows = memberLifePatternRepository.findAllLifestyleByMemberIdIn(memberIds);
        List<MatchingPreferenceConditionRow> conditionRows = preferenceConditionRepository.findAllPreferenceConditionByMemberIdIn(memberIds);
        List<MatchingPreferenceConditionWeightRow> conditionWeightRows = preferenceConditionWeightRepository.findAllPreferenceConditionWeightByMemberIdIn(memberIds);

        return calculateScores(requesterId, targetMemberIds, lifestyleRows, conditionRows, conditionWeightRows);
    }

    private Map<Long, Compatibility> calculateScores(
            Long requesterId,
            List<Long> targetMemberIds,
            List<MatchingLifestyleRow> lifestyleRows,
            List<MatchingPreferenceConditionRow> conditionRows,
            List<MatchingPreferenceConditionWeightRow> conditionWeightRows
    ) {
        if (requesterId == null || targetMemberIds == null || targetMemberIds.isEmpty()) return Map.of();

        Map<Long, Map<Long, MatchingLifestyleRow>> lifestylesByMemberId = lifestyleRows.stream()
                .filter(row -> row.lifePatternId() != null)
                .collect(Collectors.groupingBy(
                        MatchingLifestyleRow::memberId,
                        LinkedHashMap::new,
                        Collectors.toMap(
                                MatchingLifestyleRow::lifePatternId,
                                Function.identity(),
                                (first, ignored) -> first,
                                LinkedHashMap::new
                        )
                ));

        Map<Long, MatchingLifestyleRow> requesterLifestyles = lifestylesByMemberId.getOrDefault(requesterId, Map.of());
        if (requesterLifestyles.isEmpty()) return Map.of();

        Map<Long, List<MatchingPreferenceConditionRow>> requesterConditionsByPatternId = conditionRows.stream()
                .filter(row -> Objects.equals(row.memberId(), requesterId))
                .filter(row -> row.lifePatternId() != null)
                .collect(Collectors.groupingBy(MatchingPreferenceConditionRow::lifePatternId));

        Set<Long> requesterImportantPatternIds = conditionWeightRows.stream()
                .filter(row -> Objects.equals(row.memberId(), requesterId))
                .map(MatchingPreferenceConditionWeightRow::lifePatternId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, ScaleRange> scaleRangesByPatternId = findScaleRanges(requesterLifestyles.values());
        int maxRawScore = requesterLifestyles.size() + (int) requesterImportantPatternIds.stream().filter(requesterLifestyles::containsKey).count();

        if (maxRawScore == 0) return Map.of();

        return targetMemberIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        targetMemberId -> calculateCompatibility(
                                requesterLifestyles,
                                requesterConditionsByPatternId,
                                requesterImportantPatternIds,
                                lifestylesByMemberId.getOrDefault(targetMemberId, Map.of()),
                                scaleRangesByPatternId,
                                maxRawScore
                        )
                ));
    }

    @Override
    public Map<Long, Integer> calculateSimpleScores(Long requesterId, List<Long> targetMemberIds) {
        return toSimpleScores(calculateScores(requesterId, targetMemberIds));
    }

    @Override
    public Compatibility calculateScore(Long requesterId, Long targetMemberId) {
        if (targetMemberId == null) return null;
        return calculateScores(requesterId, List.of(targetMemberId)).get(targetMemberId);
    }

    @Override
    public Integer calculateSimpleScore(Long requesterId, Long targetMemberId) {
        Compatibility compatibility = calculateScore(requesterId, targetMemberId);
        return compatibility == null ? null : compatibility.getTotalScore();
    }

    public Integer calculateSnapshotScore(Long evaluatorId, Long targetId) {
        List<MemberLifePatternLog> evaluatorLogs = memberLifePatternLogRepository.findLatestLogsWithFetchByMemberId(evaluatorId);
        if (evaluatorLogs.isEmpty()) throw new BusinessException(LifePatternErrorCode.DEGREE_NOT_FOUND);

        MemberLifePatternLogDegree memberLifePatternLogDegree = evaluatorLogs.getFirst().getMemberLifePatternLogDegree();
        if (memberLifePatternLogDegree == null) throw new BusinessException(LifePatternErrorCode.DEGREE_NOT_FOUND);

        List<MemberLifePatternLog> targetLogs = memberLifePatternLogRepository.findLatestLogsWithFetchByMemberId(targetId);
        Map<Long, MemberLifePatternLog> targetLogByPatternId = toLifePatternLogMap(targetLogs);

        Map<Long, List<PreferenceConditionLog>> preferenceLogsByPatternId = findLatestPreferenceLogsByPatternId(evaluatorId);

        List<PreferenceConditionWeightLog> weightLogs = preferenceConditionWeightLogRepository.findLatestLogsWithFetchByMemberId(evaluatorId);
        Map<Long, PreferenceConditionWeightLog> weightLogByPatternId = toWeightLogMap(weightLogs);

        Map<Long, ScaleRange> scaleRangesByPatternId = findScaleRangesByPatternIds(
                evaluatorLogs.stream()
                        .map(this::lifePatternId)
                        .filter(Objects::nonNull)
                        .toList()
        );

        List<ScoreCandidate> candidates = new ArrayList<>();
        for (MemberLifePatternLog evaluatorLog : evaluatorLogs) {
            Long lifePatternId = lifePatternId(evaluatorLog);
            MemberLifePatternLog targetLog = targetLogByPatternId.get(lifePatternId);
            PreferenceConditionWeightLog weightLog = weightLogByPatternId.get(lifePatternId);
            List<PreferenceConditionLog> preferenceLogs = preferenceLogsByPatternId.getOrDefault(lifePatternId, List.of());

            if (preferenceLogs.isEmpty()) {
                candidates.add(toScoreCandidate(evaluatorLog, null, weightLog, targetLog, scaleRangesByPatternId));
                continue;
            }

            preferenceLogs.stream()
                    .map(preferenceLog -> toScoreCandidate(evaluatorLog, preferenceLog, weightLog, targetLog, scaleRangesByPatternId))
                    .forEach(candidates::add);
        }
        return calculateFinalScore(candidates);
    }

    private int calculateFinalScore(List<ScoreCandidate> candidates) {
        Map<Long, List<ScoreCandidate>> candidatesByPatternId = candidates.stream()
                .filter(candidate -> lifePatternId(candidate.lifePatternInformationLog()) != null)
                .collect(Collectors.groupingBy(
                        candidate -> lifePatternId(candidate.lifePatternInformationLog()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        int weightedScore = 0;
        int maxScore = 0;
        for (List<ScoreCandidate> group : candidatesByPatternId.values()) {
            int patternScore = group.stream()
                    .map(ScoreCandidate::score)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(0);
            int multiplier = scorePolicy.importanceMultiplier(
                    group.stream().anyMatch(candidate -> candidate.preferenceConditionWeightLog() != null)
            );
            weightedScore += patternScore * multiplier;
            maxScore += scorePolicy.getPerfectScore() * multiplier;
        }
        return scorePolicy.toScore(weightedScore, maxScore);
    }

    private ScoreCandidate toScoreCandidate(
            MemberLifePatternLog evaluatorLog,
            @Nullable PreferenceConditionLog preferenceLog,
            @Nullable PreferenceConditionWeightLog weightLog,
            @Nullable MemberLifePatternLog targetLog,
            Map<Long, ScaleRange> scaleRangesByPatternId
    ) {
        Long lifePatternId = lifePatternId(evaluatorLog);
        int score = calculateCandidateScore(
                evaluatorLog,
                preferenceLog,
                targetLog,
                scaleRangesByPatternId.get(lifePatternId)
        );
        return new ScoreCandidate(evaluatorLog, preferenceLog, weightLog, score);
    }

    private int calculateCandidateScore(
            MemberLifePatternLog evaluatorLog,
            @Nullable PreferenceConditionLog preferenceLog,
            @Nullable MemberLifePatternLog targetLog,
            @Nullable ScaleRange scaleRange
    ) {
        if (targetLog == null) {
            return 0;
        }

        LifePatternInformation criterionInformation = preferenceLog == null
                ? evaluatorLog.getLifePatternInformation()
                : preferenceLog.getLifePatternInformation();
        LifePatternInformation targetInformation = targetLog.getLifePatternInformation();
        LifePatternType type = evaluatorLog.getLifePatternInformation().getLifePattern().getDtype();

        double similarity = switch (type) {
            case SINGLE_CHOICE -> calculateSingleChoiceSimilarity(criterionInformation.getId(), targetInformation.getId());
            case BOOLEAN -> calculateBooleanSimilarity(criterionInformation.getDvalue(), targetInformation.getDvalue());
            case SCALE -> calculateScaleSimilarity(criterionInformation.getDvalue(), targetInformation.getDvalue(), scaleRange);
        };
        return scorePolicy.toPercent(similarity);
    }

    private Map<Long, MemberLifePatternLog> toLifePatternLogMap(List<MemberLifePatternLog> logs) {
        return logs.stream()
                .filter(log -> lifePatternId(log) != null)
                .collect(Collectors.toMap(
                        this::lifePatternId,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
    }

    private Map<Long, PreferenceConditionWeightLog> toWeightLogMap(List<PreferenceConditionWeightLog> logs) {
        return logs.stream()
                .filter(log -> log.getLifePattern() != null && log.getLifePattern().getId() != null)
                .collect(Collectors.toMap(
                        log -> log.getLifePattern().getId(),
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
    }

    private Map<Long, List<PreferenceConditionLog>> findLatestPreferenceLogsByPatternId(Long memberId) {
        List<Long> lifePatternInformationIds = preferenceConditionRepository.findLifeInformationIdByMemberId(memberId);
        if (lifePatternInformationIds.isEmpty()) {
            return Map.of();
        }

        return preferenceConditionLogRepository.findLatestLogsWithFetchByMemberId(memberId, lifePatternInformationIds)
                .stream()
                .filter(log -> lifePatternId(log) != null)
                .collect(Collectors.groupingBy(
                        this::lifePatternId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private Map<Long, Integer> toSimpleScores(Map<Long, Compatibility> scores) {
        return scores.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getTotalScore()
                ));
    }

    private Compatibility calculateCompatibility(
            Map<Long, MatchingLifestyleRow> requesterLifestyles,
            Map<Long, List<MatchingPreferenceConditionRow>> requesterConditionsByPatternId,
            Set<Long> requesterImportantPatternIds,
            Map<Long, MatchingLifestyleRow> targetLifestyles,
            Map<Long, ScaleRange> scaleRangesByPatternId,
            int maxRawScore
    ) {
        double rawScore = 0.0;
        List<Compatibility.LifeStyleInfo> lifeStyleInfo = new ArrayList<>();

        for (Map.Entry<Long, MatchingLifestyleRow> entry : requesterLifestyles.entrySet()) {
            Long lifePatternId = entry.getKey();
            MatchingLifestyleRow requesterLifestyle = entry.getValue();
            MatchingLifestyleRow targetLifestyle = targetLifestyles.get(lifePatternId);
            double similarity = 0.0;

            if (targetLifestyle != null) {
                similarity = calculateSimilarity(
                        requesterLifestyle,
                        requesterConditionsByPatternId.getOrDefault(lifePatternId, List.of()),
                        targetLifestyle,
                        scaleRangesByPatternId.get(lifePatternId)
                );
            }

            int importanceMultiplier = scorePolicy.importanceMultiplier(requesterImportantPatternIds.contains(lifePatternId));
            rawScore += similarity * importanceMultiplier;
            lifeStyleInfo.add(new Compatibility.LifeStyleInfo(
                    requesterLifestyle.lifePatternId(),
                    requesterLifestyle.name(),
                    scorePolicy.toPercent(similarity)
            ));
        }

        Integer score = scorePolicy.toScore(rawScore, maxRawScore);
        return new Compatibility(score, lifeStyleInfo);
    }

    private double calculateSimilarity(
            MatchingLifestyleRow requesterLifestyle,
            List<MatchingPreferenceConditionRow> requesterConditions,
            MatchingLifestyleRow targetLifestyle,
            ScaleRange scaleRange
    ) {
        LifePatternType type = targetLifestyle.type();

        if (type == LifePatternType.SINGLE_CHOICE) return calculateSingleChoiceSimilarity(requesterLifestyle, requesterConditions, targetLifestyle);
        if (type == LifePatternType.BOOLEAN) return calculateBooleanSimilarity(requesterLifestyle, requesterConditions, targetLifestyle);
        if (type == LifePatternType.SCALE) return calculateScaleSimilarity(requesterLifestyle, requesterConditions, targetLifestyle, scaleRange);

        return 0.0;
    }

    private double calculateSingleChoiceSimilarity(
            MatchingLifestyleRow requesterLifestyle,
            List<MatchingPreferenceConditionRow> requesterConditions,
            MatchingLifestyleRow targetLifestyle
    ) {
        if (requesterConditions.isEmpty()) {
            return Objects.equals(
                    requesterLifestyle.lifePatternInformationId(),
                    targetLifestyle.lifePatternInformationId()
            ) ? 1.0 : 0.0;
        }

        boolean matched = requesterConditions.stream()
                .map(MatchingPreferenceConditionRow::lifePatternInformationId)
                .anyMatch(conditionValueId -> Objects.equals(conditionValueId, targetLifestyle.lifePatternInformationId()));

        return matched ? 1.0 : 0.0;
    }

    private double calculateSingleChoiceSimilarity(Long criterionInformationId, Long targetInformationId) {
        return Objects.equals(criterionInformationId, targetInformationId) ? 1.0 : 0.0;
    }

    private double calculateBooleanSimilarity(String criterionValue, String targetValue) {
        return Objects.equals(criterionValue, targetValue) ? 1.0 : 0.0;
    }

    private double calculateBooleanSimilarity(
            MatchingLifestyleRow requesterLifestyle,
            List<MatchingPreferenceConditionRow> requesterConditions,
            MatchingLifestyleRow targetLifestyle
    ) {
        String criteriaValue = requesterConditions.stream()
                .findFirst()
                .map(MatchingPreferenceConditionRow::value)
                .orElse(requesterLifestyle.value());

        return Objects.equals(criteriaValue, targetLifestyle.value()) ? 1.0 : 0.0;
    }

    private double calculateScaleSimilarity(
            MatchingLifestyleRow requesterLifestyle,
            List<MatchingPreferenceConditionRow> requesterConditions,
            MatchingLifestyleRow targetLifestyle,
            ScaleRange scaleRange
    ) {
        Integer criteriaValue = parseInteger(
                requesterConditions.stream()
                        .findFirst()
                        .map(MatchingPreferenceConditionRow::value)
                        .orElse(requesterLifestyle.value())
        );
        Integer targetValue = parseInteger(targetLifestyle.value());

        if (criteriaValue == null || targetValue == null || scaleRange == null || scaleRange.maxGap() <= 0) {
            return Objects.equals(criteriaValue, targetValue) ? 1.0 : 0.0;
        }

        double similarity = 1.0 - ((double) Math.abs(criteriaValue - targetValue) / scaleRange.maxGap());
        return scorePolicy.clampSimilarity(similarity);
    }

    private double calculateScaleSimilarity(String criterionValue, String targetValue, ScaleRange scaleRange) {
        Integer parsedCriterionValue = parseInteger(criterionValue);
        Integer parsedTargetValue = parseInteger(targetValue);

        if (parsedCriterionValue == null || parsedTargetValue == null || scaleRange == null || scaleRange.maxGap() <= 0) {
            return Objects.equals(parsedCriterionValue, parsedTargetValue) ? 1.0 : 0.0;
        }

        double similarity = 1.0 - ((double) Math.abs(parsedCriterionValue - parsedTargetValue) / scaleRange.maxGap());
        return scorePolicy.clampSimilarity(similarity);
    }

    private Map<Long, ScaleRange> findScaleRanges(Collection<MatchingLifestyleRow> requesterLifestyles) {
        List<Long> scalePatternIds = requesterLifestyles.stream()
                .filter(row -> row.type() == LifePatternType.SCALE)
                .map(MatchingLifestyleRow::lifePatternId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (scalePatternIds.isEmpty()) return Map.of();

        Map<Long, List<Integer>> valuesByPatternId = lifePatternInformationRepository
                .findAllValueRowsByLifePatternIdIn(scalePatternIds)
                .stream()
                .collect(Collectors.groupingBy(
                        LifePatternInformationValueRow::lifePatternId,
                        Collectors.mapping(
                                row -> parseInteger(row.value()),
                                Collectors.filtering(Objects::nonNull, Collectors.toList())
                        )
                ));

        return valuesByPatternId.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            List<Integer> values = entry.getValue();
                            int min = values.stream().min(Comparator.naturalOrder()).orElse(0);
                            int max = values.stream().max(Comparator.naturalOrder()).orElse(0);
                            return new ScaleRange(min, max);
                        }
                ));
    }

    private Map<Long, ScaleRange> findScaleRangesByPatternIds(Collection<Long> lifePatternIds) {
        List<Long> distinctLifePatternIds = lifePatternIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (distinctLifePatternIds.isEmpty()) return Map.of();

        Map<Long, List<Integer>> valuesByPatternId = lifePatternInformationRepository
                .findAllValueRowsByLifePatternIdIn(distinctLifePatternIds)
                .stream()
                .collect(Collectors.groupingBy(
                        LifePatternInformationValueRow::lifePatternId,
                        Collectors.mapping(
                                row -> parseInteger(row.value()),
                                Collectors.filtering(Objects::nonNull, Collectors.toList())
                        )
                ));

        return valuesByPatternId.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            List<Integer> values = entry.getValue();
                            int min = values.stream().min(Comparator.naturalOrder()).orElse(0);
                            int max = values.stream().max(Comparator.naturalOrder()).orElse(0);
                            return new ScaleRange(min, max);
                        }
                ));
    }

    private Long lifePatternId(MemberLifePatternLog log) {
        if (log == null || log.getLifePatternInformation() == null || log.getLifePatternInformation().getLifePattern() == null) {
            return null;
        }
        return log.getLifePatternInformation().getLifePattern().getId();
    }

    private Long lifePatternId(PreferenceConditionLog log) {
        if (log == null || log.getLifePatternInformation() == null || log.getLifePatternInformation().getLifePattern() == null) {
            return null;
        }
        return log.getLifePatternInformation().getLifePattern().getId();
    }

    private Integer parseInteger(String value) {
        try {
            return value == null ? null : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<Long> includeRequester(List<Long> targetMemberIds, Long requesterId) {
        return Stream.concat(targetMemberIds.stream(), Stream.of(requesterId))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private record ScaleRange(int min, int max) {
        int maxGap() {
            return max - min;
        }
    }

    private record ScoreCandidate(
            MemberLifePatternLog lifePatternInformationLog,
            @Nullable PreferenceConditionLog preferenceConditionLog,
            @Nullable PreferenceConditionWeightLog preferenceConditionWeightLog,
            Integer score
    ) {
    }

}
