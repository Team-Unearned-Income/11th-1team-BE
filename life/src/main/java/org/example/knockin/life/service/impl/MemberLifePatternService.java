package org.example.knockin.life.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.knockin.board.dto.BoardDetailDto.Response.Lifestyle;
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

    @Transactional
    public List<MemberLifePatternLog> saveMemberLifePatternLogAll(List<MemberLifePatternLog> memberLifePatternLogList) {
        return memberLifePatternLogRepository.saveAll(memberLifePatternLogList);
    }

    @Transactional
    public List<MemberLifePattern> saveMemberLifePatternAll(List<MemberLifePattern> memberLifePatternList) {
        return memberLifePatternRepository.saveAll(memberLifePatternList);
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
    public MemberLifePatternLogDegree memberLifePatternLogDegreeSave(Long degree) {
        return memberLifePatternLogDegreeRepository.save(MemberLifePatternLogDegree.builder().degree(degree).build());
    }
}
