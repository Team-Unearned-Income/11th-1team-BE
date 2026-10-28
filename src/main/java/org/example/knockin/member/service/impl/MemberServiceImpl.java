package org.example.knockin.member.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.knockin.member.dto.BoMemberDetailDto;
import org.example.knockin.member.dto.BoMemberListDto;
import org.example.knockin.member.dto.DeleteUserDto;
import org.example.knockin.member.dto.FcmDto;
import org.example.knockin.member.dto.LogOutDto;
import org.example.knockin.member.dto.MyAccountDto;
import org.example.knockin.meta.service.impl.AlarmServiceImpl;
import org.example.knockin.meta.service.impl.PushNotificationServiceImpl;
import org.example.knockin.meta.entity.Alarm;
import org.example.knockin.meta.entity.AlarmSetting;
import org.example.knockin.meta.entity.AlarmSettingType;
import org.example.knockin.meta.entity.AlarmType;
import org.example.knockin.verification.entity.LoginProviderType;
import org.example.knockin.member.entity.Member;
import org.example.knockin.global.entity.MemberAlarmTemplate;
import org.example.knockin.member.entity.MemberRole;
import org.example.knockin.member.entity.MemberState;
import org.example.knockin.member.entity.State;
import org.example.knockin.meta.dto.AuthResponse;
import org.example.knockin.global.auth.dto.OAuth2UserInfo;
import org.example.knockin.meta.entity.Region;
import org.example.knockin.global.exception.AuthErrorCode;
import org.example.knockin.global.auth.service.Oauth2DeleteFactory;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.MemberErrorCode;
import org.example.knockin.meta.repository.AlarmSettingRepository;
import org.example.knockin.life.dto.MyPreferencesAllDto;
import org.example.knockin.life.dto.MyProfileAllDto;
import org.example.knockin.life.repository.MemberLifePatternRepository;
import org.example.knockin.member.repository.BasicInformationRepository;
import org.example.knockin.member.repository.MemberRepository;
import org.example.knockin.member.repository.row.MatchingBasicInfoRow;
import org.example.knockin.member.repository.row.MemberWithNameRow;
import org.example.knockin.member.repository.StateRepository;
import org.example.knockin.room.repository.RoomProfileRepository;
import org.example.knockin.room.entity.RoomOfferProfile;
import org.example.knockin.room.entity.RoomProfile;
import org.example.knockin.room.entity.RoomProfileType;
import org.example.knockin.room.entity.RoomSeekerProfile;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MemberServiceImpl {
    private final MemberRepository memberRepository;
    private final Oauth2DeleteFactory oauth2DeleteFactory;
    private final MemberLifePatternRepository memberLifePatternRepository;
    private final BasicInformationRepository basicInformationRepository;
    private final RoomProfileRepository roomProfileRepository;
    private final StateRepository stateRepository;
    private final AlarmSettingRepository alarmSettingRepository;
    private final AlarmServiceImpl alarmService;
    private final PushNotificationServiceImpl pushNotificationService;

    @Transactional
    public Member getOrSave(OAuth2UserInfo oAuth2UserInfo) {
        String providerId = String.valueOf(oAuth2UserInfo.getId());
        return memberRepository.findMemberByProvider(providerId, oAuth2UserInfo.getProviderType())
                .orElseGet(() -> {
                    Member newMember = Member.builder()
                            .providerType(oAuth2UserInfo.getProviderType())
                            .providerId(String.valueOf(oAuth2UserInfo.getId()))
                            .email(oAuth2UserInfo.getEmail())
                            .role(MemberRole.USER)
                            .name(oAuth2UserInfo.getName())
                            .isDelete(false)
                            .build();

                    Member resultMember = memberRepository.save(newMember);
                    List<AlarmSetting> alarmSettingList = Arrays.stream(AlarmSettingType.values()).map(item -> AlarmSetting.builder().member(resultMember).isEnabled(true).alarmSettingType(item).build()).toList();
                    alarmSettingRepository.saveAll(alarmSettingList);
                    stateRepository.save(State.builder().states(MemberState.ACTIVE).member(resultMember).build());
                    return resultMember;
                });
    }

    public AuthResponse findMemberForLogin(Member member, String accessToken) {
        AuthResponse authResponse = memberRepository.findMemberInfo(member).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        authResponse.setAccessToken(accessToken);
        return authResponse;
    }

    @Transactional
    public DeleteUserDto.Response deleteMember(String userName, LoginProviderType providerType) {
        Member member = memberRepository.findMemberByProvider(userName, providerType).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));

        try {
            if (!oauth2DeleteFactory.getDeleteService(member.getProviderType()).requestUnlink(member.getProviderId())) {
                log.warn("소셜 연결 해제 실패. 회원 탈퇴는 진행합니다. providerType: {}", member.getProviderType());
            }
        } catch (RuntimeException e) {
            log.warn("소셜 연결 해제 중 오류. 회원 탈퇴는 진행합니다. providerType: {}, errorType: {}", member.getProviderType(), e.getClass().getSimpleName());
        }

        member.delete();
        return DeleteUserDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void hardDeleteMember(List<Long> memberIds, LocalDateTime thresholdDate) {
        if (!memberIds.isEmpty()) {
            memberRepository.deleteMembersByDelete(memberIds, thresholdDate);
        }
    }

    public Optional<Member> findById(Long id) {
        return memberRepository.findById(id);
    }

    public Member findByIdOrThrow(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    public List<Member> findAllById(List<Long> ids) {
        return memberRepository.findAllById(ids);
    }

    public List<MemberWithNameRow> findAllWithNameRowById(List<Long> ids) {
        return memberRepository.findAllWithNameRowById(ids);
    }

    public List<MatchingBasicInfoRow> findMatchingBasicRow(
            List<Long> excludeMemberIds,
            int limit,
            Long likedByMemberId,
            Long requesterId
    ) {
        return memberRepository.findMatchingBasicRow(
                excludeMemberIds,
                limit,
                likedByMemberId,
                requesterId
        );
    }

    public MatchingBasicInfoRow findMatchingBasicRowById(Long memberId) {
        return memberRepository.findMatchingBasicRowById(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    public MyProfileAllDto.Response findProfileAll(Member member) {
        MyProfileAllDto.Response.UserInfo userInfo = memberRepository.findByProfile(member);
        if (userInfo.getBirth() != null) {
            userInfo.setAge(Period.between(userInfo.getBirth(), LocalDate.now()).getYears());
        }
        List<MyProfileAllDto.Response.Lifestyle> lifestyles = memberRepository.findByLifePattern(member);
        List<MyProfileAllDto.Response.Region> regions = new ArrayList<>();
        List<MyProfileAllDto.Response.RoomProfile> roomProfiles = new ArrayList<>();
        RoomProfileType type = null;
        LocalDateTime comeEnableAt = null;
        Integer deposit = null;
        Integer mounthRent = null;
        Integer minDeposit = null;
        Integer maxDeposit = null;
        Integer minMounthRent = null;
        Integer maxMounthRent = null;

        RoomProfile roomProfileEntity = memberRepository.findByRoomProfile(member).orElse(null);
        if (roomProfileEntity != null) {
            type = roomProfileEntity.getType();
            comeEnableAt = roomProfileEntity.getComeableAt();

            if (roomProfileEntity instanceof RoomOfferProfile offer) {
                deposit = offer.getDeposit();
                mounthRent = offer.getMonthlyRent();

                if (offer.getRegion() != null) {
                    MyProfileAllDto.Response.Region regDto = new MyProfileAllDto.Response.Region();
                    regDto.setRegionId(offer.getRegion().getId());
                    regDto.setRegion(getFullRegionName(offer.getRegion()));
                    regions.add(regDto);
                }

                roomProfiles = memberRepository.findRoomTypes(offer);
            } else if (roomProfileEntity instanceof RoomSeekerProfile seeker) {
                minDeposit = seeker.getMinDeposit();
                maxDeposit = seeker.getMaxDeposit();
                minMounthRent = seeker.getMinMonthlyRent();
                maxMounthRent = seeker.getMaxMonthlyRent();

                List<Region> seekerRegions = memberRepository.findSeekerRegionEntities(seeker);
                for (Region region : seekerRegions) {
                    MyProfileAllDto.Response.Region regDto = new MyProfileAllDto.Response.Region();
                    regDto.setRegionId(region.getId());
                    regDto.setRegion(getFullRegionName(region));
                    regions.add(regDto);
                }
                roomProfiles = memberRepository.findRoomTypes(seeker);
            }
        }

        return MyProfileAllDto.Response.builder()
                .lifestyles(lifestyles)
                .type(type)
                .comeEnableAt(comeEnableAt)
                .deposit(deposit)
                .mounthRent(mounthRent)
                .minDeposit(minDeposit)
                .maxDeposit(maxDeposit)
                .minMounthRent(minMounthRent)
                .maxMounthRent(maxMounthRent)
                .region(regions)
                .roomProfile(roomProfiles)
                .userInfo(userInfo)
                .build();
    }

    public MyPreferencesAllDto.Response findPreAll(Member member) {
        return MyPreferencesAllDto.Response.builder()
                .lifestyles(memberRepository.findPreferenceLifeStyle(member))
                .conditions(memberRepository.findPreferenceCondition(member))
                .build();
    }

    public boolean isOnBoarding(Member member) {
        return roomProfileRepository.isExsitRoomProfile(member) && memberLifePatternRepository.isExsitLifeStyle(member.getId()) && basicInformationRepository.isExsitBasicInformation(member);
    }

    private String getFullRegionName(Region regionEntity) {
        if (regionEntity == null) {
            return "";
        }

        List<String> regionNames = new ArrayList<>();
        Region current = regionEntity;

        while (current != null) {
            regionNames.add(0, current.getName());
            current = current.getParent();
        }

        return String.join(" ", regionNames);
    }

    public BoMemberListDto.Response findBackOfficeMemberList(Pageable pageable, BoMemberListDto.Request request) {
        return BoMemberListDto.Response.builder().memberInfoList(memberRepository.findBackOfficeMemberList(pageable, request)).build();
    }

    public BoMemberDetailDto.Response findBackOfficeMember(Long id) {
        return memberRepository.findBackOfficeMember(id);
    }

    @Transactional
    public State setMemberState(Member member, MemberState memberState, String rejectReason) {
        State state = stateRepository.findByMember(member).getFirst();

        if(memberState.equals(MemberState.ACTIVE)) {
            state.activeState();
            Alarm alarm = Alarm.builder()
                    .title(MemberAlarmTemplate.MEMBER_ACTIVE.formatTitle())
                    .contents(MemberAlarmTemplate.MEMBER_ACTIVE.formatContents())
                    .isRead(false)
                    .member(member)
                    .expiredAt(LocalDateTime.now().plusDays(1))
                    .type(AlarmType.DEFAULT)
                    .build();

            alarmService.sendToClient(member.getId(), MemberAlarmTemplate.MEMBER_ACTIVE.name(), alarm);
            pushNotificationService.send(member, AlarmSettingType.NOTIFICATION, MemberAlarmTemplate.MEMBER_ACTIVE.formatTitle(), MemberAlarmTemplate.MEMBER_ACTIVE.formatContents(), MemberAlarmTemplate.MEMBER_ACTIVE.formatDeepLink());
        } else {
            state.rejectState(rejectReason);
        }

        return state;
    }

    @Transactional
    public Member setMemberAuth(Member member, MemberRole memberRole) {
        member.changeRole(memberRole);
        return member;
    }

    @Transactional
    public FcmDto.Response upsertFcmProps(Long memberId, FcmDto.Request request) {
        Member member = findByIdOrThrow(memberId);
        member.setFcmProps(request.getDeviceId(), request.getFcmToken(), request.getPlatform());
        return FcmDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    public MyProfileAllDto.Response.UserInfo findProfileInfo(Member member) {
        return memberRepository.findByProfile(member);
    }

    public MyAccountDto.Response findMyAccountRole(Long memberId) {
        Member member = findByIdOrThrow(memberId);
        return MyAccountDto.Response.builder().role(member.getRole()).providerType(member.getProviderType()).build();
    }

    public State findStateByMemberId(Long memberId) {
        return stateRepository.findByMemberId(memberId).getFirst();
    }

    public void validateMemberState(Long memberId) {
        State state = findStateByMemberId(memberId);
        if (!state.getStates().equals(MemberState.ACTIVE)) {
            throw new BusinessException(MemberErrorCode.NOT_ACTIVE_MEMBER);
        }
    }

    @Transactional
    public LogOutDto.Response logout(Long id) {
        Member member = findByIdOrThrow(id);
        member.clearFcmProps();
        return LogOutDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }
}
