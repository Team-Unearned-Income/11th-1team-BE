package org.example.knockin.mate.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.MemberErrorCode;
import org.example.knockin.global.exception.MyRoommateErrorCode;
import org.example.knockin.mate.dto.HouseRuleDetailDto;
import org.example.knockin.mate.dto.HouseRuleDto;
import org.example.knockin.mate.dto.HouseRuleListDto;
import org.example.knockin.mate.entity.MyRoommate;
import org.example.knockin.mate.entity.RoommateHouseRule;
import org.example.knockin.mate.entity.RoommateMatchingRequired;
import org.example.knockin.mate.repository.RoommateHouseRuleRepository;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class HouseRuleServiceImpl {

    private final RoommateHouseRuleRepository roommateHouseRuleRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public HouseRuleDto.Response save(MyRoommate myRoommate, HouseRuleDto.Request request, Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
        RoommateHouseRule roommateHouseRule = RoommateHouseRule.builder()
                .member(member)
                .myRoommate(myRoommate)
                .title(request.getTitle())
                .contents(request.getContents())
                .build();
        roommateHouseRuleRepository.save(roommateHouseRule);
        return HouseRuleDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional(readOnly = true)
    public List<HouseRuleListDto.Response> findList(MyRoommate myRoommate) {
        List<RoommateHouseRule> myRoommateRules = roommateHouseRuleRepository.findByMyRoommateIdAndIsDeleted(myRoommate.getId(), false);
        return myRoommateRules.stream().map(this::toListDto).toList();
    }

    private HouseRuleListDto.Response toListDto(RoommateHouseRule roommateHouseRule) {
        return HouseRuleListDto.Response.builder()
                .id(roommateHouseRule.getId())
                .title(roommateHouseRule.getTitle())
                .contents(roommateHouseRule.getContents())
                .build();
    }

    @Transactional(readOnly = true)
    public HouseRuleDetailDto.Response findDetail(Long memberId, Long houseRuleId) {
        RoommateHouseRule roommateHouseRule = roommateHouseRuleRepository.findWithFetchedById(houseRuleId).orElseThrow(() -> new BusinessException(MyRoommateErrorCode.HOUSE_RULE_NOT_FOUND));
        validateHouseRule(memberId, roommateHouseRule);

        return HouseRuleDetailDto.Response.builder()
                .id(roommateHouseRule.getId())
                .title(roommateHouseRule.getTitle())
                .contents(roommateHouseRule.getContents())
                .build();
    }

    @Transactional
    public HouseRuleDto.Response modify(Long memberId, Long houseRuleId, HouseRuleDto.Request request) {
        RoommateHouseRule roommateHouseRule = roommateHouseRuleRepository.findWithFetchedById(houseRuleId).orElseThrow(() -> new BusinessException(MyRoommateErrorCode.HOUSE_RULE_NOT_FOUND));
        validateHouseRule(memberId, roommateHouseRule);
        roommateHouseRule.modify(request.getTitle(), request.getContents());
        return HouseRuleDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public HouseRuleDto.Response delete(Long memberId, Long houseRuleId) {
        RoommateHouseRule roommateHouseRule = roommateHouseRuleRepository.findWithFetchedById(houseRuleId).orElseThrow(() -> new BusinessException(MyRoommateErrorCode.HOUSE_RULE_NOT_FOUND));
        validateHouseRule(memberId, roommateHouseRule);
        roommateHouseRule.softDelete();
        return HouseRuleDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    private void validateHouseRule(Long memberId, RoommateHouseRule roommateHouseRule) {
        RoommateMatchingRequired roommateMatchingRequired = roommateHouseRule.getMyRoommate().getRoommateMatchingRequired();
        Long requesterId = roommateMatchingRequired.getRequester().getId();
        Long requesteeId = roommateMatchingRequired.getRequestee().getId();

        if (!Objects.equals(memberId, requesterId) && !Objects.equals(memberId, requesteeId))
            throw new BusinessException(MyRoommateErrorCode.HOUSE_RULE_ACCESS_DENIED);
    }
}
