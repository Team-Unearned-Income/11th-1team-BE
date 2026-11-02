package org.example.knockin.mate.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.example.knockin.verification.service.impl.AuthenticationServiceImpl;
import org.example.knockin.declaration.service.impl.DeclarationServiceImpl;
import org.example.knockin.util.dto.Compatibility;
import org.example.knockin.util.dto.MatchDetailDto;
import org.example.knockin.util.dto.MatchDto;
import org.example.knockin.util.dto.MatchListDto;
import org.example.knockin.util.dto.MatchListDto.Condition;
import org.example.knockin.util.dto.MatchListDto.ConditionWeight;
import org.example.knockin.util.dto.MatchListDto.Lifestyle;
import org.example.knockin.declaration.dto.MemberReportDto;
import org.example.knockin.verification.entity.AuthenticationType;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.service.impl.MemberInterestServiceImpl;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.example.knockin.room.entity.RoomProfileType;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.CommonErrorCode;
import org.example.knockin.global.util.DateUtils;
import org.example.knockin.global.util.HasMemberId;
import org.example.knockin.global.util.StringUtils;
import org.example.knockin.life.service.impl.MemberLifePatternService;
import org.example.knockin.life.service.impl.PreferenceConditionServiceImpl;
import org.example.knockin.verification.repository.row.MemberAuthenticationRow;
import org.example.knockin.life.repository.row.MatchingLifestyleRow;
import org.example.knockin.life.repository.row.MatchingPreferenceConditionRow;
import org.example.knockin.life.repository.row.MatchingPreferenceConditionWeightRow;
import org.example.knockin.member.repository.row.MatchingBasicInfoRow;
import org.example.knockin.room.repository.row.MatchingOfferProfileRow;
import org.example.knockin.room.repository.row.MatchingSeekerProfileRow;
import org.example.knockin.room.repository.row.MatchingSeekerRegionRow;
import org.example.knockin.room.repository.row.MatchingSeekerRoomTypeRow;
import org.example.knockin.room.service.impl.RoomOfferProfileServiceImpl;
import org.example.knockin.room.service.impl.RoomSeekerProfileServiceImpl;
import org.example.knockin.mate.service.RoommateMatchingService;
import org.example.knockin.util.service.RoommateScoreService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoommateMatchingServiceImpl implements RoommateMatchingService {
    private final MemberServiceImpl memberService;
    private final MemberInterestServiceImpl memberInterestService;
    private final RoomSeekerProfileServiceImpl roomSeekerProfileService;
    private final RoomOfferProfileServiceImpl roomOfferProfileService;
    private final MemberLifePatternService memberLifePatternService;
    private final PreferenceConditionServiceImpl preferenceConditionService;
    private final AuthenticationServiceImpl authenticationService;
    private final DeclarationServiceImpl declarationService;
    private final RoommateScoreService javaRoommateScoreV2Service;

    @Override
    @Transactional(readOnly = true)
    public Slice<MatchListDto.Response> findMatchingList(Long memberId, MatchListDto.Request request) {
        validateLikedOnlyRequest(request.getLikedOnly(), memberId);
        int size = request.getSize();
        List<Long> excludeMemberIds = resolveExcludeMemberIds(memberId, request);
        Long likedByMemberId = Boolean.TRUE.equals(request.getLikedOnly()) ? memberId : null;

        List<MatchingBasicInfoRow> rawRows = memberService.findMatchingBasicRow(excludeMemberIds, size + 1, likedByMemberId, memberId);
        boolean hasNext = rawRows.size() > size;
        List<MatchingBasicInfoRow> matchingListBasicRow = rawRows.stream().limit(size).toList();
        List<Long> memberIds = matchingListBasicRow.stream().map(MatchingBasicInfoRow::memberId).toList();

        if (memberIds.isEmpty()) {
            return new SliceImpl<>(List.of(), PageRequest.of(0, size), false);
        }

        List<MatchingOfferProfileRow> matchingOfferProfileRows = roomOfferProfileService.findAllOfferProfileByMemberIdIn(memberIds);

        List<MatchingSeekerProfileRow> matchingSeekerProfileRows = roomSeekerProfileService.findAllSeekerProfileByMemberIdIn(memberIds);
        List<MatchingSeekerRegionRow> matchingSeekerRegionRows = roomSeekerProfileService.findAllSeekerRegionByMemberIdIn(memberIds);
        List<MatchingSeekerRoomTypeRow> matchingSeekerRoomTypeRows = roomSeekerProfileService.findAllSeekerRoomTypeByMemberIdIn(memberIds);

        List<MatchingLifestyleRow> matchingLifestyleRows = memberLifePatternService.findMatchingRowByMemberIdsIn(memberIds);
        List<MatchingPreferenceConditionRow> matchingPreferenceConditionRows = preferenceConditionService.findRowByMemberIdsIn(memberIds);
        List<MatchingPreferenceConditionWeightRow> matchingPreferenceConditionWeightRows = preferenceConditionService.findWeightRowByMemberIdsIn(memberIds);
        List<MemberAuthenticationRow> authenticationRows = authenticationService.findAcceptedByMemberIds(memberIds);
        Map<Long, Compatibility> scoresByMemberId = Optional.ofNullable(javaRoommateScoreV2Service.calculateScores(memberId, memberIds)).orElse(Map.of());

        Map<Long, MatchingOfferProfileRow> offerMap = HasMemberId.toMapByMemberId(matchingOfferProfileRows);
        Map<Long, MatchingSeekerProfileRow> seekerMap = HasMemberId.toMapByMemberId(matchingSeekerProfileRows);

        Map<Long, List<String>> seekerRegionMap = HasMemberId.groupingByMemberId(
                matchingSeekerRegionRows,
                row -> StringUtils.parseToRegionFullName(
                        row.grandParentRegionName(),
                        row.parentRegionName(),
                        row.regionName()
                )
        );

        Map<Long, List<String>> seekerRoomTypeMap = HasMemberId.groupingByMemberId(
                matchingSeekerRoomTypeRows,
                MatchingSeekerRoomTypeRow::roomTypeName
        );

        Map<Long, List<MatchListDto.Lifestyle>> lifestyleMap = HasMemberId.groupingByMemberId(
                matchingLifestyleRows,
                this::toLifestyle
        );

        Map<Long, List<MatchListDto.Condition>> conditionMap = HasMemberId.groupingByMemberId(
                matchingPreferenceConditionRows,
                this::toCondition
        );

        Map<Long, List<MatchListDto.ConditionWeight>> conditionWeightMap = HasMemberId.groupingByMemberId(
                matchingPreferenceConditionWeightRows,
                this::toConditionWeight
        );

        Map<Long, List<AuthenticationType>> authenticationMap = authenticationRows.stream()
                .collect(Collectors.groupingBy(
                        MemberAuthenticationRow::memberId,
                        Collectors.mapping(MemberAuthenticationRow::type, Collectors.toList())
                ));

        Set<Long> likedMemberIds = findLikedMemberIds(memberId, memberIds);

        List<MatchListDto.Response> responses = matchingListBasicRow.stream()
                .map(row -> toResponse(
                        row,
                        offerMap,
                        seekerMap,
                        seekerRegionMap,
                        seekerRoomTypeMap,
                        lifestyleMap,
                        conditionMap,
                        conditionWeightMap,
                        authenticationMap,
                        likedMemberIds,
                        scoresByMemberId
                ))
                .toList();

        return new SliceImpl<>(
                responses,
                PageRequest.of(0, size),
                hasNext
        );
    }

    private void validateLikedOnlyRequest(Boolean likedOnly, Long memberId) {
        if (Boolean.TRUE.equals(likedOnly) && memberId == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    private List<Long> resolveExcludeMemberIds(Long memberId, MatchListDto.Request request) {
        List<Long> excludeMemberIds = new ArrayList<>();

        if (request != null && request.getExcludeMemberIds() != null) {
            excludeMemberIds.addAll(request.getExcludeMemberIds());
        }

        if (memberId != null) {
            excludeMemberIds.add(memberId);
        }

        return excludeMemberIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Set<Long> findLikedMemberIds(Long memberId, List<Long> memberIds) {
        if (memberId == null || memberIds.isEmpty()) {
            return Set.of();
        }

        return Set.copyOf(memberInterestService.findActiveReceiverIdsBySenderIdAndReceiverIds(memberId, memberIds));
    }

    private MatchListDto.Lifestyle toLifestyle(MatchingLifestyleRow row) {
        return MatchListDto.Lifestyle.builder()
                .lifestyleId(row.lifestyleId())
                .name(row.name())
                .value(row.value())
                .description(row.description())
                .type(row.type())
                .imageUrl(row.imageUrl())
                .build();
    }

    private MatchListDto.Condition toCondition(MatchingPreferenceConditionRow row) {
        return MatchListDto.Condition.builder()
                .conditionId(row.conditionId())
                .name(row.name())
                .value(row.value())
                .description(row.description())
                .type(row.type())
                .imageUrl(row.imageUrl())
                .build();
    }

    private MatchListDto.ConditionWeight toConditionWeight(MatchingPreferenceConditionWeightRow row) {
        return MatchListDto.ConditionWeight.builder()
                .conditionWeightId(row.conditionWeightId())
                .name(row.name())
                .imageUrl(row.imageUrl())
                .build();
    }

    private MatchListDto.Response toResponse(
            MatchingBasicInfoRow row,
            Map<Long, MatchingOfferProfileRow> offerMap,
            Map<Long, MatchingSeekerProfileRow> seekerMap,
            Map<Long, List<String>> seekerRegionMap,
            Map<Long, List<String>> seekerRoomTypeMap,
            Map<Long, List<MatchListDto.Lifestyle>> lifestyleMap,
            Map<Long, List<MatchListDto.Condition>> conditionMap,
            Map<Long, List<MatchListDto.ConditionWeight>> conditionWeightMap,
            Map<Long, List<AuthenticationType>> authenticationMap,
            Set<Long> likedMemberIds,
            Map<Long, Compatibility> scoresByMemberId
    ) {
        Long candidateId = row.memberId();

        MatchListDto.OfferProfile offerProfile = null;
        MatchListDto.SeekerProfile seekerProfile = null;

        if (row.roomProfileType() == RoomProfileType.OFFER) {
            MatchingOfferProfileRow offerProfileRow = offerMap.get(candidateId);
            offerProfile = toOfferProfile(offerProfileRow);
        }

        if (row.roomProfileType() == RoomProfileType.SEEKER) {
            MatchingSeekerProfileRow matchingSeekerProfileRow = seekerMap.get(candidateId);
            List<String> regionFullNames = seekerRegionMap.getOrDefault(candidateId, List.of());
            List<String> roomTypeNames = seekerRoomTypeMap.getOrDefault(candidateId, List.of());
            seekerProfile = toSeekerProfile(matchingSeekerProfileRow, roomTypeNames, regionFullNames);
        }

        return MatchListDto.Response.builder()
                .memberId(candidateId)
                .memberProfileImageUrl(row.memberProfileImageUrl())
                .memberName(row.memberName())
                .memberAge(DateUtils.calculateAge(row.birth()))
                .gender(row.gender())
                .interested(likedMemberIds.contains(candidateId))
                .roomProfileType(row.roomProfileType())
                .offerProfile(offerProfile)
                .seekerProfile(seekerProfile)
                .score(extractScore(scoresByMemberId.get(candidateId)))
                .lifeStyles(lifestyleMap.getOrDefault(candidateId, List.of()))
                .conditions(conditionMap.getOrDefault(candidateId, List.of()))
                .conditionWeights(conditionWeightMap.getOrDefault(candidateId, List.of()))
                .authentications(authenticationMap.getOrDefault(candidateId, List.of()))
                .build();
    }

    private MatchListDto.OfferProfile toOfferProfile(MatchingOfferProfileRow row) {
        if (row == null) {
            return null;
        }

        String regionFullName = StringUtils.parseToRegionFullName(
                row.grandParentRegionName(),
                row.parentRegionName(),
                row.regionName()
        );

        return MatchListDto.OfferProfile.builder()
                .deposit(row.deposit())
                .monthlyRent(row.monthlyRent())
                .regionFullName(regionFullName)
                .roomTypeName(row.roomTypeName())
                .build();
    }

    private MatchListDto.SeekerProfile toSeekerProfile(MatchingSeekerProfileRow row, List<String> roomTypeNames, List<String> regionFullNames) {
        if (row == null) {
            return null;
        }

        return MatchListDto.SeekerProfile.builder()
                .minDeposit(row.minDeposit())
                .maxDeposit(row.maxDeposit())
                .minMonthlyRent(row.minMonthlyRent())
                .maxMonthlyRent(row.maxMonthlyRent())
                .roomTypeNames(roomTypeNames)
                .regionFullNames(regionFullNames)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MatchDetailDto.Response findMatchingDetail(Long targetMemberId, Long requesterId) {
        MatchingBasicInfoRow basicInfoRow = memberService.findMatchingBasicRowById(targetMemberId);

        List<AuthenticationType> authenticationTypes = authenticationService.findTypesByMemberId(targetMemberId);

        RoomProfileType roomProfileType = basicInfoRow.roomProfileType();
        MatchingOfferProfileRow offerProfileRow = null;
        MatchingSeekerProfileRow seekerProfileRow = null;
        List<MatchingSeekerRegionRow> seekerRegionRows = List.of();
        List<MatchingSeekerRoomTypeRow> seekerRoomTypeRows = List.of();

        if (roomProfileType == RoomProfileType.OFFER) {
            offerProfileRow = roomOfferProfileService.findAllOfferProfileByMemberIdIn(List.of(targetMemberId))
                    .stream()
                    .findFirst()
                    .orElse(null);
        }

        if (roomProfileType == RoomProfileType.SEEKER) {
            seekerProfileRow = roomSeekerProfileService.findAllSeekerProfileByMemberIdIn(List.of(targetMemberId))
                    .stream()
                    .findFirst()
                    .orElse(null);
            seekerRegionRows = roomSeekerProfileService.findAllSeekerRegionByMemberIdIn(List.of(targetMemberId));
            seekerRoomTypeRows = roomSeekerProfileService.findAllSeekerRoomTypeByMemberIdIn(List.of(targetMemberId));
        }

        List<MatchingLifestyleRow> lifestyleRows = memberLifePatternService.findMatchingRowByMemberIdsIn(List.of(targetMemberId));
        List<MatchingPreferenceConditionRow> preferenceConditionRows = preferenceConditionService.findRowByMemberIdsIn(List.of(targetMemberId));
        List<MatchingPreferenceConditionWeightRow> preferenceConditionWeightRows = preferenceConditionService.findWeightRowByMemberIdsIn(List.of(targetMemberId));
        Compatibility compatibility = javaRoommateScoreV2Service.calculateScore(requesterId, targetMemberId);

        boolean interested = requesterId != null && memberInterestService.existsActiveBySenderIdAndReceiverId(requesterId, targetMemberId);

        List<String> roomTypeNames = seekerRoomTypeRows.stream().map(MatchingSeekerRoomTypeRow::roomTypeName).toList();
        List<String> regionFullNames = seekerRegionRows.stream()
                .map(row -> StringUtils.parseToRegionFullName(row.grandParentRegionName(), row.parentRegionName(), row.regionName()
                ))
                .toList();

        List<Lifestyle> lifestyles = lifestyleRows.stream()
                .filter(row -> Objects.equals(row.memberId(), targetMemberId))
                .map(this::toLifestyle)
                .toList();

        List<Condition> conditions = preferenceConditionRows.stream()
                .filter(row -> Objects.equals(row.memberId(), targetMemberId))
                .map(this::toCondition)
                .toList();

        List<ConditionWeight> conditionWeights = preferenceConditionWeightRows.stream()
                .filter(row -> Objects.equals(row.memberId(), targetMemberId))
                .map(this::toConditionWeight)
                .toList();

        boolean mine = targetMemberId.equals(requesterId);

        return MatchDetailDto.Response.builder()
                .memberId(basicInfoRow.memberId())
                .memberProfileImageUrl(basicInfoRow.memberProfileImageUrl())
                .memberName(basicInfoRow.memberName())
                .memberAge(DateUtils.calculateAge(basicInfoRow.birth()))
                .gender(basicInfoRow.gender())
                .interested(interested)
                .roomProfileType(roomProfileType)
                .offerProfile(roomProfileType == RoomProfileType.OFFER ? toOfferProfile(offerProfileRow) : null)
                .seekerProfile(roomProfileType == RoomProfileType.SEEKER ? toSeekerProfile(seekerProfileRow, roomTypeNames, regionFullNames) : null)
                .lifeStyles(lifestyles)
                .conditions(conditions)
                .conditionWeights(conditionWeights)
                .authentications(authenticationTypes)
                .compatibility(compatibility)
                .mine(mine)
                .build();
    }

    private Integer extractScore(Compatibility compatibility) {
        return compatibility == null ? null : compatibility.getTotalScore();
    }

    @Override
    @Transactional
    public MatchDto.Response likeMatching(Long senderId, Long receiverId) {
        Member sender = memberService.findByIdOrThrow(senderId);
        Member receiver = memberService.findByIdOrThrow(receiverId);
        memberInterestService.toggle(sender, receiver);

        return new MatchDto.Response(LocalDateTime.now());
    }

    @Override
    @Transactional
    public MemberReportDto.Response reportMatching(Long reporterId, Long reportedId, MemberReportDto.Request request) {
        Member reporter = memberService.findByIdOrThrow(reporterId);
        Member reported = memberService.findByIdOrThrow(reportedId);
        declarationService.reportMember(reporter.getId(), reported.getId(), request.getContents());

        return new MemberReportDto.Response(LocalDateTime.now());
    }

}
