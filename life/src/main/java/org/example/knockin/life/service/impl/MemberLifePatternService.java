package org.example.knockin.life.service.impl;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.example.knockin.board.dto.BoardDetailDto.Response.Lifestyle;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.OnBoardErrorCode;
import org.example.knockin.life.dto.ModifyProfileLifeStyleDto;
import org.example.knockin.life.entity.LifePatternInformation;
import org.example.knockin.life.entity.MemberLifePattern;
import org.example.knockin.life.entity.MemberLifePatternLog;
import org.example.knockin.life.entity.MemberLifePatternLogDegree;
import org.example.knockin.life.repository.MemberLifePatternLogDegreeRepository;
import org.example.knockin.life.repository.MemberLifePatternLogRepository;
import org.example.knockin.life.repository.MemberLifePatternRepository;
import org.example.knockin.life.repository.row.MatchingLifestyleRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberLifePatternService {
    private final MemberLifePatternRepository memberLifePatternRepository;
    private final MemberLifePatternLogRepository memberLifePatternLogRepository;
    private final MemberLifePatternLogDegreeRepository memberLifePatternLogDegreeRepository;
    private final LifeStyleServiceImpl lifeStyleService;

    @Transactional
    public List<MemberLifePatternLog> saveMemberLifePatternLogAll(List<MemberLifePatternLog> memberLifePatternLogList) {
        return memberLifePatternLogRepository.saveAll(memberLifePatternLogList);
    }

    @Transactional
    public List<MemberLifePattern> saveMemberLifePatternAll(List<MemberLifePattern> memberLifePatternList) {
        return memberLifePatternRepository.saveAll(memberLifePatternList);
    }

    @Transactional
    public List<Long> saveMemberLifePatterns(Long memberId, List<Long> lifePatternIds) {
        List<MemberLifePattern> existing = findByMemberId(memberId);
        if (!existing.isEmpty()) {
            deleteMemberLifePatternAll(existing);
        }
        List<MemberLifePattern> memberLifePatternList = new ArrayList<>();
        List<MemberLifePatternLog> memberLifePatternLogList = new ArrayList<>();
        MemberLifePatternLogDegree memberLifePatternLogDegree = saveDegreeByMemberId(memberId);

        lifeStyleService.findLifePatternInformationByIds(lifePatternIds).forEach(item -> {
            memberLifePatternList.add(MemberLifePattern.builder().memberId(memberId).lifePatternInformation(item).build());
            memberLifePatternLogList.add(MemberLifePatternLog.builder().memberId(memberId).lifePatternInformation(item).memberLifePatternLogDegree(memberLifePatternLogDegree).build());
        });

        saveMemberLifePatternLogAll(memberLifePatternLogList);
        return saveMemberLifePatternAll(memberLifePatternList).stream().map(MemberLifePattern::getId).toList();
    }

    @Transactional
    public void modifyLifeStyleLog(Long memberId) {
        MemberLifePatternLogDegree memberLifePatternLogDegree = saveDegreeByMemberId(memberId);
        List<MemberLifePattern> memberLifePatternList = findByMemberId(memberId);
        List<MemberLifePatternLog> logList = memberLifePatternList.stream().map(pattern ->
                MemberLifePatternLog.builder().memberId(memberId).lifePatternInformation(pattern.getLifePatternInformation()).memberLifePatternLogDegree(memberLifePatternLogDegree).build()).toList();

        if (!logList.isEmpty()) saveMemberLifePatternLogAll(logList);
    }

    @Transactional
    public void deleteMemberLifePatternAll(List<MemberLifePattern> memberLifePatternList) {
        memberLifePatternRepository.deleteAll(memberLifePatternList);
        memberLifePatternRepository.flush();
    }

    public List<MemberLifePattern> findByMemberId(Long memberId) {
        return memberLifePatternRepository.findByMemberId(memberId);
    }

    public List<MatchingLifestyleRow> findMatchingRowByMemberIdsIn(List<Long> memberIds) {
        return memberLifePatternRepository.findAllLifestyleByMemberIdIn(memberIds);
    }

    public List<Lifestyle> findLifeStyleDtoByMemberId(Long memberId) {
        return memberLifePatternRepository.getLifeStyleDto(memberId);
    }

    public Long findMaxmemberLifePatternLogDegree(Long memberId) {
        return memberLifePatternLogDegreeRepository.findMaxmemberLifePatternLogDegree(memberId).orElse(null);
    }

    @Transactional
    public MemberLifePatternLogDegree saveDegreeByMemberId(Long memberId) {
        Long maxDegree = memberLifePatternLogDegreeRepository.findMaxmemberLifePatternLogDegree(memberId).orElse(null);
        long degree = maxDegree != null ? maxDegree + 1 : 1L;
        return memberLifePatternLogDegreeRepository.save(MemberLifePatternLogDegree.builder().degree(degree).build());
    }

    @Transactional
    public void modifyLifeStyle(ModifyProfileLifeStyleDto.Request request, Long memberId) {
        List<Long> memberLifePatternIds = request.getLifestyles().stream()
                .map(ModifyProfileLifeStyleDto.Request.LifeStyleInfo::getId)
                .toList();

        List<Long> newLifestyleIds = request.getLifestyles().stream()
                .map(ModifyProfileLifeStyleDto.Request.LifeStyleInfo::getLifestyleId)
                .toList();

        Map<Long, LifePatternInformation> newInfoMap = lifeStyleService.findLifePatternInformationAllById(newLifestyleIds).stream()
                .collect(Collectors.toMap(LifePatternInformation::getId, info -> info));

        List<MemberLifePattern> memberLifePatternList = findByMemberId(memberId);
        List<MemberLifePattern> modifyMemberLifePatternList = memberLifePatternList.stream()
                .filter(item -> memberLifePatternIds.contains(item.getId()))
                .toList();

        modifyMemberLifePatternList.forEach(item -> {
            request.getLifestyles().forEach(data -> {
                if (Objects.equals(data.getId(), item.getId())) {
                    LifePatternInformation newInfo = newInfoMap.get(data.getLifestyleId());

                    if(!Objects.equals(newInfo.getLifePattern().getId(), item.getLifePatternInformation().getLifePattern().getId())) {
                        throw new BusinessException(OnBoardErrorCode.ONBOARD_LIFE_STYLE_VAILDATION_FAIL);
                    }

                    item.modifyLifePatternInformation(newInfo);
                }
            });
        });
    }

}
