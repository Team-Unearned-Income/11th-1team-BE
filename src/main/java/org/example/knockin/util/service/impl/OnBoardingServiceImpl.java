package org.example.knockin.util.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.knockin.agreement.service.AgreementService;
import org.example.knockin.mate.service.impl.MetaServiceImpl;
import org.example.knockin.verification.dto.MyVerificationListDto;
import org.example.knockin.verification.service.impl.AuthenticationServiceImpl;
import org.example.knockin.board.dto.MyBoardListDto;
import org.example.knockin.member.dto.ModifyProfileAllDto;
import org.example.knockin.member.dto.ModifyProfileBasicDto;
import org.example.knockin.member.dto.ProfileVisibilityDto;
import org.example.knockin.member.dto.SaveProfileAllDto;
import org.example.knockin.member.dto.SaveProfileBasicDto;
import org.example.knockin.member.entity.BasicInformationFile;
import org.example.knockin.member.service.impl.BasicInformationServiceImpl;
import org.example.knockin.member.service.impl.MemberPrivacyServiceImpl;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.example.knockin.meta.entity.File;
import org.example.knockin.meta.entity.FileType;
import org.example.knockin.member.entity.BasicInformation;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberPrivacy;
import org.example.knockin.member.entity.MemberPrivacyType;
import org.example.knockin.meta.entity.Region;
import org.example.knockin.global.exception.AuthErrorCode;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.FileErrorCode;
import org.example.knockin.global.exception.MetaErrorCode;
import org.example.knockin.global.exception.OnBoardErrorCode;
import org.example.knockin.life.service.impl.MemberLifePatternService;
import org.example.knockin.life.service.impl.PreferenceConditionServiceImpl;
import org.example.knockin.life.dto.ModifyPreferencesAllDto;
import org.example.knockin.life.dto.ModifyPreferencesConditionsDto;
import org.example.knockin.life.dto.ModifyPreferencesLifeStyleDto;
import org.example.knockin.life.dto.ModifyProfileLifeStyleDto;
import org.example.knockin.life.dto.MyPreferencesAllDto;
import org.example.knockin.life.dto.MyProfileAllDto;
import org.example.knockin.life.dto.SavePreferencesAllDto;
import org.example.knockin.life.dto.SavePreferencesConditionsDto;
import org.example.knockin.life.dto.SavePreferencesLifeStyleDto;
import org.example.knockin.life.dto.SaveProfileLifeStyleDto;
import org.example.knockin.mate.service.impl.MyRoomMateServiceImpl;
import org.example.knockin.room.dto.ModifyProfileRoomInfoDto;
import org.example.knockin.room.dto.SaveProfileRoomInfoDto;
import org.example.knockin.room.entity.OfferRoomType;
import org.example.knockin.room.entity.RoomOfferProfile;
import org.example.knockin.room.entity.RoomProfile;
import org.example.knockin.room.entity.RoomProfileType;
import org.example.knockin.room.entity.RoomSeekerProfile;
import org.example.knockin.room.entity.RoomSeekerProfileRegion;
import org.example.knockin.room.entity.RoomType;
import org.example.knockin.room.entity.SeekerRoomType;
import org.example.knockin.room.service.impl.RoomProfileServiceImpl;
import org.example.knockin.room.service.impl.RoomSeekerProfileRegionServiceImpl;
import org.example.knockin.room.service.impl.RoomTypeServiceImpl;
import org.example.knockin.meta.service.FileService;
import org.example.knockin.board.service.RoommateBoardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OnBoardingServiceImpl {
    private final AuthenticationServiceImpl authenticationService;
    private final BasicInformationServiceImpl basicInformationService;
    private final MemberServiceImpl memberService;
    private final MemberLifePatternService memberLifePatternService;
    private final RoomProfileServiceImpl roomProfileService;
    private final RoomTypeServiceImpl roomTypeService;
    private final RoomSeekerProfileRegionServiceImpl roomSeekerProfileRegionService;
    private final MetaServiceImpl metaService;
    private final PreferenceConditionServiceImpl preferenceConditionService;
    private final MemberPrivacyServiceImpl memberPrivacyService;
    private final MyRoomMateServiceImpl myRoomMateService;
    private final RoommateBoardService roommateBoardService;
    private final FileService fileService;
    private final AgreementService agreementService;

    private String resolveName(String name) {
        if (StringUtils.hasText(name)) {
            return name;
        }
        return "유저_" + UUID.randomUUID().toString().substring(0, 6);
    }

    @Transactional
    public BasicInformation saveBasicInfo(SaveProfileBasicDto.Request request, Member member) {
        String resolvedName = StringUtils.hasText(member.getName()) ? member.getName() : resolveName(request.getName());
        String resolvedEmail = StringUtils.hasText(member.getEmail()) ? member.getEmail() : request.getEmail();

        List<BasicInformation> existing = basicInformationService.findByMember(member);
        if (!existing.isEmpty()) {
            BasicInformation basicInformation = existing.getFirst();
            ModifyProfileBasicDto.Request modifyRequest = ModifyProfileBasicDto.Request.builder()
                    .name(resolvedName)
                    .birth(request.getBirth())
                    .gender(request.getGender())
                    .email(resolvedEmail)
                    .build();
            basicInformation.modifyBasicInformation(modifyRequest);
            return basicInformation;
        }
        BasicInformation basicInformation = BasicInformation.builder().member(member).name(resolvedName).birth(request.getBirth()).gender(request.getGender()).email(resolvedEmail).build();
        return basicInformationService.save(basicInformation);
    }

    @Transactional
    public SaveProfileBasicDto.Response saveBasicInfoLogic(SaveProfileBasicDto.Request request, Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        if(ObjectUtils.isEmpty(saveBasicInfo(request, member))) throw new BusinessException(OnBoardErrorCode.ONBOARD_BASIC_SAVE_ERROR);
        if(agreementService.saveMemberAgreement(request.getTerms(), memberId)) throw new BusinessException(OnBoardErrorCode.ONBOARD_TERM_SAVE_ERROR);
        saveOrUpdateState(member);
        return SaveProfileBasicDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public SaveProfileLifeStyleDto.Response saveLifeStyleLogic(SaveProfileLifeStyleDto.Request request, Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        if(memberLifePatternService.saveMemberLifePatterns(member.getId(), request.getLifestyles()).isEmpty()) throw new BusinessException(OnBoardErrorCode.ONBOARD_LIFE_STYLE_SAVE_ERROR);
        saveOrUpdateState(member);
        return SaveProfileLifeStyleDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public RoomProfile saveRoomInfo(SaveProfileRoomInfoDto.Request request, Member member) {
        List<RoomProfile> existing = roomProfileService.findByMember(member);
        if (!existing.isEmpty()) {
            ModifyProfileRoomInfoDto.Request modifyRequest = ModifyProfileRoomInfoDto.Request.builder()
                    .type(request.getType())
                    .minDeposit(request.getMinDeposit())
                    .maxDeposit(request.getMaxDeposit())
                    .minMounthRent(request.getMinMounthRent())
                    .maxMounthRent(request.getMaxMounthRent())
                    .comeEnableAt(request.getComeEnableAt())
                    .region(request.getRegion())
                    .roomProfile(request.getRoomProfile())
                    .deposit(request.getDeposit())
                    .mounthRent(request.getMounthRent())
                    .isComeableAtNegotiable(request.isComeableAtNegotiable())
                    .build();
            modifyRoomInfo(modifyRequest, member);
            return roomProfileService.findByMember(member).getFirst();
        }
        RoomProfile roomProfile = null;

        switch (request.getType()) {
            case OFFER -> {
                List<OfferRoomType> offerRoomTypeList = new ArrayList<>();

                Region region = metaService.findByRegionId(request.getRegion().getFirst()).orElseThrow(() -> new BusinessException(
                        MetaErrorCode.REGION_NOT_FOUND));
                RoomOfferProfile roomOfferProfile = roomProfileService.save(RoomOfferProfile.builder()
                        .member(member)
                        .region(region)
                        .deposit(request.getDeposit())
                        .monthlyRent(request.getMounthRent())
                        .isComeableAtNegotiable(request.isComeableAtNegotiable())
                        .comeableAt(request.getComeEnableAt()).build());
                roomProfile = roomOfferProfile;

                metaService.findByRoomTypes(request.getRoomProfile()).forEach(item -> {
                    offerRoomTypeList.add(OfferRoomType.builder().roomType(item).roomOfferProfile(roomOfferProfile).build());
                });
                roomTypeService.saveOfferRoomTypeAll(offerRoomTypeList);
            }
            case SEEKER -> {
                List<SeekerRoomType> seekerRoomTypes = new ArrayList<>();
                List<RoomSeekerProfileRegion> roomSeekerProfileRegionList = new ArrayList<>();

                RoomSeekerProfile roomSeekerProfile = roomProfileService.save(RoomSeekerProfile.builder()
                        .member(member)
                        .minDeposit(request.getMinDeposit())
                        .maxDeposit(request.getMaxDeposit())
                        .minMonthlyRent(request.getMinMounthRent())
                        .maxMonthlyRent(request.getMaxMounthRent())
                        .isComeableAtNegotiable(request.isComeableAtNegotiable())
                        .comeableAt(request.getComeEnableAt()).build());
                roomProfile = roomSeekerProfile;

                metaService.findByRegions(request.getRegion()).forEach(item -> {
                    roomSeekerProfileRegionList.add(
                            RoomSeekerProfileRegion.builder().roomSeekerProfile(roomSeekerProfile).region(item).build());
                });
                roomSeekerProfileRegionService.saveAll(roomSeekerProfileRegionList);

                metaService.findByRoomTypes(request.getRoomProfile()).forEach(item -> {
                    seekerRoomTypes.add(SeekerRoomType.builder().roomType(item).roomSeekerProfile(roomSeekerProfile).build());
                });
                roomTypeService.saveSeekerRoomTypeAll(seekerRoomTypes);
            }
        }

        return roomProfile;
    }

    @Transactional
    public SaveProfileRoomInfoDto.Response saveRoomInfoLogic(SaveProfileRoomInfoDto.Request request, Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        if(ObjectUtils.isEmpty(saveRoomInfo(request,member))) throw new BusinessException(OnBoardErrorCode.ONBOARD_ROOM_INFO_SAVE_ERROR);
        saveOrUpdateState(member);
        return SaveProfileRoomInfoDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public SaveProfileAllDto.Response saveAll(SaveProfileAllDto.Request request, Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));

        SaveProfileBasicDto.Request basicRequest = SaveProfileBasicDto.Request.builder()
                .name(request.getName())
                .birth(request.getBirth())
                .gender(request.getGender())
                .email(request.getEmail())
                .terms(request.getTerms())
                .build();

        SaveProfileLifeStyleDto.Request lifeStyleRequest = SaveProfileLifeStyleDto.Request.builder()
                .lifestyles(request.getLifestyles())
                .build();

        SaveProfileRoomInfoDto.Request roomInfoRequest = SaveProfileRoomInfoDto.Request.builder()
                .type(request.getType())
                .minDeposit(request.getMinDeposit())
                .maxDeposit(request.getMaxDeposit())
                .minMounthRent(request.getMinMounthRent())
                .maxMounthRent(request.getMaxMounthRent())
                .comeEnableAt(request.getComeEnableAt())
                .region(request.getRegion())
                .roomProfile(request.getRoomProfile())
                .deposit(request.getDeposit())
                .mounthRent(request.getMounthRent())
                .isComeableAtNegotiable(request.isComeableAtNegotiable())
                .build();

        if(ObjectUtils.isEmpty(saveBasicInfo(basicRequest, member))) throw new BusinessException(OnBoardErrorCode.ONBOARD_BASIC_SAVE_ERROR);
        if(agreementService.saveMemberAgreement(basicRequest.getTerms(), memberId)) throw new BusinessException(OnBoardErrorCode.ONBOARD_TERM_SAVE_ERROR);
        if(memberLifePatternService.saveMemberLifePatterns(member.getId(), lifeStyleRequest.getLifestyles()).isEmpty()) throw new BusinessException(OnBoardErrorCode.ONBOARD_LIFE_STYLE_SAVE_ERROR);
        if(ObjectUtils.isEmpty(saveRoomInfo(roomInfoRequest,member))) throw new BusinessException(OnBoardErrorCode.ONBOARD_ROOM_INFO_SAVE_ERROR);
        saveOrUpdateState(member);

        return SaveProfileAllDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public void modifyBasicInfo(ModifyProfileBasicDto.Request request, Member member) {
        BasicInformation basicInformation = basicInformationService.findByMember(member).getFirst();
        basicInformation.modifyBasicInformation(request);
    }

    @Transactional(rollbackFor = Exception.class)
    public void modifyBasicInfo(ModifyProfileBasicDto.Request request, Member member, MultipartFile file) {
        try {
            BasicInformation basicInformation = basicInformationService.findByMember(member).getFirst();
            basicInformation.modifyBasicInformation(request);

            if(file != null && !file.isEmpty()) {
                File fileEntity = fileService.save(file, FileType.USER_PROFILE_IMAGE);
                BasicInformationFile basicInformationFile = basicInformationService.findBasicInformationFile(basicInformation);
                if(basicInformationFile != null) {
                    basicInformationFile.modifyFile(fileEntity.getId());
                } else {
                    basicInformationService.save(BasicInformationFile.builder()
                            .fileId(fileEntity.getId())
                            .basicInformation(basicInformation)
                            .build());
                }
            }
        } catch (IOException e) {
            log.info("파일 저장에 실패하였습니다. {}", e.toString());
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Transactional
    public ModifyProfileBasicDto.Response modifyBasicInfoLogic(ModifyProfileBasicDto.Request request, Long memberId, MultipartFile file) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));

        modifyBasicInfo(request, member, file);
        agreementService.modifyMemberAgreement(request.getTerms(), memberId);

        return ModifyProfileBasicDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public ModifyProfileLifeStyleDto.Response modifyLifeStyleLogic(ModifyProfileLifeStyleDto.Request request, Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        memberLifePatternService.modifyLifeStyle(request, member.getId());
        memberLifePatternService.modifyLifeStyleLog(member.getId());
        return ModifyProfileLifeStyleDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public void modifyRoomInfo(ModifyProfileRoomInfoDto.Request request, Member member) {
        RoomProfile roomProfile = roomProfileService.findByMember(member).getFirst();
        RoomProfileType targetType = request.getType();

        if (roomProfile.getType() != targetType) {
            if (roomProfile instanceof RoomOfferProfile roomOfferProfile) {
                roomTypeService.deleteByRoomOfferProfile(roomOfferProfile);
            } else if (roomProfile instanceof RoomSeekerProfile seekerProfile) {
                roomTypeService.deleteByRoomSeekerProfile(seekerProfile);
                roomSeekerProfileRegionService.deleteByRoomSeekerProfile(seekerProfile);
            }

            roomProfileService.delete(roomProfile);

            if (targetType == RoomProfileType.SEEKER) {
                RoomSeekerProfile newSeekerProfile = roomProfileService.save(RoomSeekerProfile.builder()
                        .member(member)
                        .minDeposit(request.getMinDeposit())
                        .maxDeposit(request.getMaxDeposit())
                        .minMonthlyRent(request.getMinMounthRent())
                        .maxMonthlyRent(request.getMaxMounthRent())
                        .isComeableAtNegotiable(request.isComeableAtNegotiable())
                        .comeableAt(request.getComeEnableAt())
                        .build());

                List<RoomSeekerProfileRegion> seekerRegions = metaService.findByRegions(request.getRegion()).stream().map(region -> RoomSeekerProfileRegion.builder().roomSeekerProfile(newSeekerProfile).region(region).build()).toList();
                roomSeekerProfileRegionService.saveAll(seekerRegions);

                List<SeekerRoomType> seekerRoomTypes = metaService.findByRoomTypes(request.getRoomProfile()).stream().map(roomType -> SeekerRoomType.builder().roomSeekerProfile(newSeekerProfile).roomType(roomType).build()).toList();
                roomTypeService.saveSeekerRoomTypeAll(seekerRoomTypes);
            } else if (targetType == RoomProfileType.OFFER) {
                Region region = metaService.findByRegionId(request.getRegion().getFirst())
                        .orElseThrow(() -> new BusinessException(MetaErrorCode.REGION_NOT_FOUND));

                RoomOfferProfile newOfferProfile = roomProfileService.save(RoomOfferProfile.builder()
                        .member(member)
                        .region(region)
                        .deposit(request.getDeposit())
                        .monthlyRent(request.getMounthRent())
                        .isComeableAtNegotiable(request.isComeableAtNegotiable())
                        .comeableAt(request.getComeEnableAt())
                        .build());

                List<OfferRoomType> offerRoomTypes = metaService.findByRoomTypes(request.getRoomProfile()).stream().map(roomType -> OfferRoomType.builder().roomOfferProfile(newOfferProfile).roomType(roomType).build()).toList();
                roomTypeService.saveOfferRoomTypeAll(offerRoomTypes);
            }
        } else {
            List<RoomType> roomTypeList = metaService.findByRoomTypes(request.getRoomProfile());

            if (roomProfile instanceof RoomOfferProfile roomOfferProfile) {
                Region region = metaService.findByRegionId(request.getRegion().getFirst()).orElseThrow(() -> new BusinessException(MetaErrorCode.REGION_NOT_FOUND));

                roomOfferProfile.updateOffer(request, region);
                roomTypeService.deleteByRoomOfferProfile(roomOfferProfile);

                List<OfferRoomType> offerRoomTypeList = roomTypeList.stream().map(item -> OfferRoomType.builder().roomType(item).roomOfferProfile(roomOfferProfile).build()).toList();
                roomTypeService.saveOfferRoomTypeAll(offerRoomTypeList);
            } else if (roomProfile instanceof RoomSeekerProfile seekerProfile) {
                List<Region> regionList = metaService.findByRegions(request.getRegion());

                seekerProfile.updateSeeker(request);
                roomTypeService.deleteByRoomSeekerProfile(seekerProfile);

                List<SeekerRoomType> seekerRoomTypes = roomTypeList.stream().map(item -> SeekerRoomType.builder().roomType(item).roomSeekerProfile(seekerProfile).build()).toList();
                roomTypeService.saveSeekerRoomTypeAll(seekerRoomTypes);

                roomSeekerProfileRegionService.deleteByRoomSeekerProfile(seekerProfile);

                List<RoomSeekerProfileRegion> roomSeekerProfileRegionList = regionList.stream().map(item -> RoomSeekerProfileRegion.builder().roomSeekerProfile(seekerProfile).region(item).build()).toList();
                roomSeekerProfileRegionService.saveAll(roomSeekerProfileRegionList);
            } else {
                throw new BusinessException(OnBoardErrorCode.ONBOARD_ROOM_INFO_VAILDATION_FAIL);
            }
        }
    }

    @Transactional
    public ModifyProfileRoomInfoDto.Response modifyRoomInfoLogic(ModifyProfileRoomInfoDto.Request request, Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        modifyRoomInfo(request, member);
        return ModifyProfileRoomInfoDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public ModifyProfileAllDto.Response modifyAll(ModifyProfileAllDto.Request request, Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));

        ModifyProfileBasicDto.Request basicRequest = ModifyProfileBasicDto.Request.builder()
                .name(request.getName())
                .birth(request.getBirth())
                .gender(request.getGender())
                .email(request.getEmail())
                .terms(request.getTerms())
                .build();

        List<ModifyProfileLifeStyleDto.Request.LifeStyleInfo> mappedLifestyles = request.getLifestyles().stream()
                .map(info -> {
                    ModifyProfileLifeStyleDto.Request.LifeStyleInfo targetInfo = new ModifyProfileLifeStyleDto.Request.LifeStyleInfo();
                    targetInfo.setId(info.getId());
                    targetInfo.setLifestyleId(info.getLifestyleId());
                    return targetInfo;
                }).toList();

        ModifyProfileLifeStyleDto.Request lifeStyleRequest = ModifyProfileLifeStyleDto.Request.builder()
                .lifestyles(mappedLifestyles)
                .build();

        ModifyProfileRoomInfoDto.Request roomInfoRequest = ModifyProfileRoomInfoDto.Request.builder()
                .type(request.getType())
                .minDeposit(request.getMinDeposit())
                .maxDeposit(request.getMaxDeposit())
                .minMounthRent(request.getMinMounthRent())
                .maxMounthRent(request.getMaxMounthRent())
                .comeEnableAt(request.getComeEnableAt())
                .isComeableAtNegotiable(request.isComeableAtNegotiable())
                .region(request.getRegion())
                .roomProfile(request.getRoomProfile())
                .deposit(request.getDeposit())
                .mounthRent(request.getMounthRent())
                .build();

        modifyBasicInfo(basicRequest, member);
        agreementService.modifyMemberAgreement(basicRequest.getTerms(), memberId);
        memberLifePatternService.modifyLifeStyle(lifeStyleRequest, member.getId());
        memberLifePatternService.modifyLifeStyleLog(member.getId());
        modifyRoomInfo(roomInfoRequest, member);

        return ModifyProfileAllDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public List<Long> savePreferenceLifeStyle(SavePreferencesLifeStyleDto.Request request, Member member) {
        preferenceConditionService.deletePreferenceConditionByMemberId(member.getId());
        return preferenceConditionService.saveAll(member.getId(), request.getLifestyles());
    }

    @Transactional
    public List<Long> savePreferenceLifeStyleLog(SavePreferencesLifeStyleDto.Request request, Member member) {
        Long degree = preferenceConditionService.findNextLogDegree(member.getId());
        Long degreeId = preferenceConditionService.saveLogDegree(degree);
        return preferenceConditionService.saveLogsAll(member.getId(), degreeId, request.getLifestyles());
    }

    @Transactional
    public SavePreferencesLifeStyleDto.Response savePreferenceLifeStyleLogic(SavePreferencesLifeStyleDto.Request request, Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));

        if(savePreferenceLifeStyle(request, member).isEmpty()) throw new BusinessException(OnBoardErrorCode.ONBOARD_PREFERENCE_STEP1_SAVE_ERROR);
        if(savePreferenceLifeStyleLog(request, member).isEmpty()) throw new BusinessException(OnBoardErrorCode.ONBOARD_PREFERENCE_STEP1_LOG_SAVE_ERROR);

        return SavePreferencesLifeStyleDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public List<Long>  savePreferenceCondition(SavePreferencesConditionsDto.Request request, Member member) {
        preferenceConditionService.deletePreferenceConditionWeightByMemberId(member.getId());
        return preferenceConditionService.savePreferenceConditionWeights(member.getId(), request.getConditions());
    }

    @Transactional
    public List<Long>  savePreferenceConditionLog(SavePreferencesConditionsDto.Request request, Member member) {
        Long degree = preferenceConditionService.findNextWeightLogDegreeNumber(member.getId());
        Long degreeId = preferenceConditionService.savePreferenceConditionWeightLogDegree(degree);
        return preferenceConditionService.savePreferenceConditionWeightLogs(member.getId(), request.getConditions(), degreeId);
    }

    @Transactional
    public SavePreferencesConditionsDto.Response savePreferenceConditionLogic(SavePreferencesConditionsDto.Request request, Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));

        if(savePreferenceCondition(request, member).isEmpty()) throw new BusinessException(OnBoardErrorCode.ONBOARD_PREFERENCE_STEP2_SAVE_ERROR);
        if(savePreferenceConditionLog(request, member).isEmpty()) throw new BusinessException(OnBoardErrorCode.ONBOARD_PREFERENCE_STEP2_LOG_SAVE_ERROR);

        return SavePreferencesConditionsDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public SavePreferencesAllDto.Response savePreferenceAll(SavePreferencesAllDto.Request request, Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));

        SavePreferencesLifeStyleDto.Request lifeStyleRequest = new SavePreferencesLifeStyleDto.Request(request.getLifestyles());
        SavePreferencesConditionsDto.Request conditionRequest = new SavePreferencesConditionsDto.Request(request.getConditions());

        if(savePreferenceLifeStyle(lifeStyleRequest, member).isEmpty()) throw new BusinessException(OnBoardErrorCode.ONBOARD_PREFERENCE_STEP1_SAVE_ERROR);
        if(savePreferenceLifeStyleLog(lifeStyleRequest, member).isEmpty()) throw new BusinessException(OnBoardErrorCode.ONBOARD_PREFERENCE_STEP1_LOG_SAVE_ERROR);
        if(savePreferenceCondition(conditionRequest, member).isEmpty()) throw new BusinessException(OnBoardErrorCode.ONBOARD_PREFERENCE_STEP2_SAVE_ERROR);
        if(savePreferenceConditionLog(conditionRequest, member).isEmpty()) throw new BusinessException(OnBoardErrorCode.ONBOARD_PREFERENCE_STEP2_LOG_SAVE_ERROR);

        return SavePreferencesAllDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    // FIXME
    @Transactional
    public void modifyPreferenceLifeStyleLog(Member member) {
        Long degree = preferenceConditionService.findNextLogDegree(member.getId());
        Long degreeId = preferenceConditionService.saveLogDegree(degree);
        preferenceConditionService.saveLogsAll(member.getId(), degreeId);
    }

    @Transactional
    public ModifyPreferencesLifeStyleDto.Response modifyPreLifeStyleLogic(ModifyPreferencesLifeStyleDto.Request request, Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));

        preferenceConditionService.modifyPreferenceLifeStyle(request,member);
        modifyPreferenceLifeStyleLog(member);

        return ModifyPreferencesLifeStyleDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public void modifyPreCondition(ModifyPreferencesConditionsDto.Request request, Member member) {
        preferenceConditionService.deletePreferenceConditionWeightByMemberId(member.getId());
        preferenceConditionService.savePreferenceConditionWeights(member.getId(), request.getConditions());
    }

    @Transactional
    public void modifyPreConditionLog(Member member) {
        Long degreeNextNumber = preferenceConditionService.findNextWeightLogDegreeNumber(member.getId());
        Long degreeId = preferenceConditionService.savePreferenceConditionWeightLogDegree(degreeNextNumber);
        preferenceConditionService.modifyPreferenceConditionWeightLog(member.getId(), degreeId);
    }

    @Transactional
    public ModifyPreferencesConditionsDto.Response modifyPreConditionLogic(ModifyPreferencesConditionsDto.Request request, Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));

        modifyPreCondition(request, member);
        modifyPreConditionLog(member);

        return ModifyPreferencesConditionsDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public ModifyPreferencesAllDto.Response modifyPreAll(ModifyPreferencesAllDto.Request request, Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));

        ModifyPreferencesLifeStyleDto.Request lifeStyleRequest = ModifyPreferencesLifeStyleDto.Request.builder().lifestyles(request.getLifestyles()).build();
        ModifyPreferencesConditionsDto.Request conditionRequest = ModifyPreferencesConditionsDto.Request.builder().conditions(request.getConditions()).build();

        preferenceConditionService.modifyPreferenceLifeStyle(lifeStyleRequest,member);
        modifyPreferenceLifeStyleLog(member);
        modifyPreCondition(conditionRequest, member);
        modifyPreConditionLog(member);

        return ModifyPreferencesAllDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    public MyProfileAllDto.Response findProfileAll(Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        return memberService.findProfileAll(member);
    }

    public MyPreferencesAllDto.Response findPreAll(Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        return memberService.findPreAll(member);
    }

    @Transactional
    public ProfileVisibilityDto.Response changeProfileStatus(ProfileVisibilityDto.Request request, Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));

        if(myRoomMateService.isExistRoomMate(member)) throw new BusinessException(OnBoardErrorCode.ONBOARD_PROFILE_STATE_CHANGE_ERROR);
        memberPrivacyService.findByMember(member).getFirst().changeState(request.getStatus());

        return ProfileVisibilityDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    public MyBoardListDto.Response findMyBoardList(Pageable pageable, Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        Page<MyBoardListDto.Response.BoardItem> pageResult = roommateBoardService.getMyBoardList(pageable, member);
        return MyBoardListDto.Response.builder().boards(pageResult.getContent()).build();
    }

    @Transactional
    public void saveOrUpdateState(Member member) {
        MemberPrivacyType targetType = memberService.isOnBoarding(member) ? MemberPrivacyType.PUBLIC : MemberPrivacyType.PRIVATE;

        List<MemberPrivacy> privacyList = memberPrivacyService.findByMember(member);
        if (!privacyList.isEmpty()) {
            privacyList.getFirst().changeState(targetType);
        }
        else {
            memberPrivacyService.save(MemberPrivacy.builder().member(member).type(targetType).build());
        }
    }

    public MyProfileAllDto.Response.UserInfo findProfileInfo(Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        return memberService.findProfileInfo(member);
    }

    public MyVerificationListDto.Response findVerificationList(Pageable pageable, Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        return authenticationService.findVerificationList(pageable, member.getId());
    }
}
