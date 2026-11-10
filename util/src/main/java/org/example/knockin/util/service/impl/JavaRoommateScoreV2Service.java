package org.example.knockin.util.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.knockin.global.exception.AuthErrorCode;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.EtcErrorCode;
import org.example.knockin.life.entity.*;
import org.example.knockin.life.repository.*;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.repository.MemberRepository;
import org.example.knockin.util.dto.Compatibility;
import org.example.knockin.util.service.LifePatternTypeScoreCalc;
import org.example.knockin.util.service.RoommateScoreService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class JavaRoommateScoreV2Service implements RoommateScoreService {
    private final MemberLifePatternRepository memberLifePatternRepository;
    private final PreferenceConditionRepository preferenceConditionRepository;
    private final PreferenceConditionWeightRepository preferenceConditionWeightRepository;
    private final List<LifePatternTypeScoreCalc> lifePatternTypeScoreCalcList;
    private final MemberRepository memberRepository;

    private static final Long TOTAL_POINT = 100L;
    private static final Integer NON_PREFERENCE_WEIGHT = 1;
    private static final Integer PREFERENCE_WEIGHT = 2;

    @Override
    public Map<Long, Compatibility> calculateScores(Long requesterId, List<Long> targetMemberIds) {
        if (requesterId == null || targetMemberIds == null || targetMemberIds.isEmpty()) return Map.of();

        Map<Long, Compatibility> resultArray = new HashMap<>();
        memberRepository.findById(requesterId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        List<LifePatternInformation> myPreferenceList = preferenceConditionRepository.findByMemberId(requesterId).stream().map(PreferenceCondition::getLifePatternInformation).filter(Objects::nonNull).toList();
        List<LifePatternInformation> myDefaultList = memberLifePatternRepository.findByMemberId(requesterId).stream().map(MemberLifePattern::getLifePatternInformation).filter(Objects::nonNull).toList();
        List<LifePatternInformation> myFinalLifePatternInformationList = Stream.concat(myPreferenceList.stream(), myDefaultList.stream()).filter(item -> item != null && item.getLifePattern() != null && item.getLifePattern().getId() != null)
                .collect(Collectors.toMap(item -> item.getLifePattern().getId(), item -> item, (preferenceVal, defaultVal) -> preferenceVal)).values().stream().toList();
        List<PreferenceConditionWeight> preferenceConditionWeightList = preferenceConditionWeightRepository.findAllByMemberId(requesterId);

        List<Member> targetList = memberRepository.findAllById(targetMemberIds);
        Map<Long, List<LifePatternInformation>> targetDefaultPatternMapById = memberLifePatternRepository.findAllByMemberIdIn(targetList.stream().map(Member::getId).toList()).stream().filter(item -> item.getMemberId() != null && item.getLifePatternInformation() != null)
                .collect(Collectors.groupingBy(MemberLifePattern::getMemberId, Collectors.mapping(MemberLifePattern::getLifePatternInformation, Collectors.toList())));

        for(Member target : targetList) {
            List<LifePatternInformation> targetLifePatternInformation = targetDefaultPatternMapById.getOrDefault(target.getId(), List.of());
            Compatibility compatibility = calculateScores(myFinalLifePatternInformationList, targetLifePatternInformation, preferenceConditionWeightList);
            resultArray.put(target.getId(), compatibility);
        }

        return resultArray;
    }

    @Override
    public Map<Long, Integer> calculateSimpleScores(Long requesterId, List<Long> targetMemberIds) {
        return calculateScores(requesterId, targetMemberIds).entrySet().stream().filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getTotalScore()));
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

    public Compatibility calculateScores(List<LifePatternInformation> me, List<LifePatternInformation> target, List<PreferenceConditionWeight> preferenceConditionWeightList) {
        Map<Long, LifePatternInformation> targetMapByPatternId =
                target.stream().collect(Collectors.toMap(item -> item.getLifePattern().getId(), item -> item, (first, ignored) -> first));
        int lifePatternMaxSize = Math.max(me.size(), target.size());
        double lifePatternPartPoint = (double) TOTAL_POINT / (lifePatternMaxSize + preferenceConditionWeightList.size());
        List<Compatibility.LifeStyleInfo> lifeStyleInfoList = new ArrayList<>();
        double totalScore = 0;

        for(LifePatternInformation myInfo : me) {
            LifePattern lifePattern = myInfo.getLifePattern();
            Long patternId = lifePattern.getId();
            LifePatternInformation targetInfo = targetMapByPatternId.get(patternId);

            if (targetInfo != null) {
                Compatibility.LifeStyleInfo lifeStyleInfo = calculateScores(myInfo, targetInfo, lifePattern);
                lifeStyleInfoList.add(lifeStyleInfo);
                int preferenceWeight = preferenceConditionWeightList.stream().anyMatch(weight -> weight.getLifePattern().getId() == patternId) ? PREFERENCE_WEIGHT : NON_PREFERENCE_WEIGHT;
                totalScore += ((lifePatternPartPoint * preferenceWeight) * ((double) lifeStyleInfo.getPercent() / TOTAL_POINT));
            } else {
                lifeStyleInfoList.add(Compatibility.LifeStyleInfo.builder().id(patternId).name(lifePattern.getName()).percent(0).build());
            }
        }

        return Compatibility.builder().totalScore((int) Math.round(totalScore)).lifeStyleInfo(lifeStyleInfoList).build();
    }

    public Compatibility.LifeStyleInfo calculateScores(LifePatternInformation me, LifePatternInformation target, LifePattern lifePattern) {
        if(!me.getLifePattern().equals(target.getLifePattern())) throw new BusinessException(EtcErrorCode.SCORE_CALC_ERROR);

        Integer similarity = lifePatternTypeScoreCalcList.stream()
                .filter(item -> item.supports() == lifePattern.getDtype()).findFirst()
                .map(calc -> calc.calculateSimilarity(me, target, lifePattern))
                .orElseThrow(() -> new BusinessException(EtcErrorCode.SCORE_CALC_ERROR));

        return Compatibility.LifeStyleInfo.builder().id(lifePattern.getId()).name(lifePattern.getName()).percent(similarity).build();
    }
}
