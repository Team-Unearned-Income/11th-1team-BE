package org.example.knockin.service.impl;
 
import org.example.knockin.board.dto.MyBoardListDto;
import org.example.knockin.agreement.service.impl.AgreementServiceImpl;
import org.example.knockin.mate.service.impl.MetaServiceImpl;
import org.example.knockin.member.dto.ModifyProfileBasicDto;
import org.example.knockin.member.dto.ModifyProfileAllDto;
import org.example.knockin.member.dto.ProfileVisibilityDto;
import org.example.knockin.member.dto.SaveProfileAllDto;
import org.example.knockin.member.dto.SaveProfileBasicDto;
import org.example.knockin.member.entity.BasicInformation;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberPrivacy;
import org.example.knockin.member.entity.MemberPrivacyType;
import org.example.knockin.member.service.impl.BasicInformationServiceImpl;
import org.example.knockin.member.service.impl.MemberPrivacyServiceImpl;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.example.knockin.meta.entity.Region;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.AuthErrorCode;
import org.example.knockin.global.exception.OnBoardErrorCode;
import org.example.knockin.board.service.RoommateBoardService;
import org.example.knockin.life.service.impl.MemberLifePatternService;
import org.example.knockin.life.service.impl.PreferenceConditionServiceImpl;
import org.example.knockin.life.dto.ModifyProfileLifeStyleDto;
import org.example.knockin.life.dto.SavePreferencesConditionsDto;
import org.example.knockin.life.dto.SaveProfileLifeStyleDto;
import org.example.knockin.mate.service.impl.MyRoomMateServiceImpl;
import org.example.knockin.room.dto.ModifyProfileRoomInfoDto;
import org.example.knockin.room.dto.SaveProfileRoomInfoDto;
import org.example.knockin.room.entity.RoomOfferProfile;
import org.example.knockin.room.entity.RoomProfileType;
import org.example.knockin.room.entity.RoomSeekerProfile;
import org.example.knockin.room.entity.RoomType;
import org.example.knockin.room.service.impl.RoomProfileServiceImpl;
import org.example.knockin.room.service.impl.RoomSeekerProfileRegionServiceImpl;
import org.example.knockin.room.service.impl.RoomTypeServiceImpl;
import org.example.knockin.util.service.impl.OnBoardingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;
 
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
 
@ExtendWith(MockitoExtension.class)
class OnBoardingServiceImplTest {
 
    @Mock private BasicInformationServiceImpl basicInformationService;
    @Mock private MemberServiceImpl memberService;
    @Mock private AgreementServiceImpl agreementService;
    @Mock private MemberLifePatternService memberLifePatternService;
    @Mock private RoomProfileServiceImpl roomProfileService;
    @Mock private RoomTypeServiceImpl roomTypeService;
    @Mock private RoomSeekerProfileRegionServiceImpl roomSeekerProfileRegionService;
    @Mock private MetaServiceImpl metaService;
    @Mock private PreferenceConditionServiceImpl preferenceConditionService;
    @Mock private MemberPrivacyServiceImpl memberPrivacyService;
    @Mock private MyRoomMateServiceImpl myRoomMateService;
    @Mock private RoommateBoardService roommateBoardService;
 
    @InjectMocks
    private OnBoardingServiceImpl onBoardingService;
 
    private Member member;
    private Long memberId = 1L;
 
    @BeforeEach
    void setUp() {
        member = Member.builder().id(memberId).build();
        lenient().when(memberService.findById(memberId)).thenReturn(Optional.of(member));
        lenient().when(memberService.isOnBoarding(any())).thenReturn(true);
        lenient().when(memberPrivacyService.findByMember(any())).thenReturn(Collections.emptyList());
    }
 
    @Test
    @DisplayName("기본 정보 저장 성공 테스트")
    void saveBasicInfoLogic_Success() {
        SaveProfileBasicDto.Request request = SaveProfileBasicDto.Request.builder()
                .name("홍길동")
                .terms(List.of(1L))
                .build();
 
        given(basicInformationService.save(any(BasicInformation.class))).willReturn(mock(BasicInformation.class));
 
        SaveProfileBasicDto.Response response = onBoardingService.saveBasicInfoLogic(request, memberId);
 
        assertThat(response).isNotNull();
        verify(basicInformationService).save(any(BasicInformation.class));
        verify(agreementService).saveMemberAgreement(request.getTerms(), memberId);
        verify(memberPrivacyService).save(any());
    }
 
    @Test
    @DisplayName("기본 정보 저장 성공 테스트 (이미 존재할 시 업데이트)")
    void saveBasicInfoLogic_Upsert_Success() {
        SaveProfileBasicDto.Request request = SaveProfileBasicDto.Request.builder()
                .name("이몽룡")
                .terms(List.of(1L))
                .build();

        BasicInformation existingBasic = mock(BasicInformation.class);
        given(basicInformationService.findByMember(any())).willReturn(List.of(existingBasic));
        
        SaveProfileBasicDto.Response response = onBoardingService.saveBasicInfoLogic(request, memberId);

        assertThat(response).isNotNull();
        verify(existingBasic).modifyBasicInformation(any());
        verify(basicInformationService, never()).save(any(BasicInformation.class));
        verify(agreementService).saveMemberAgreement(request.getTerms(), memberId);
    }

    @Test
    @DisplayName("약관 저장에 실패하면 프로필 공개 상태를 변경하지 않는다")
    void saveBasicInfoLogic_FailsWhenAgreementSaveFails() {
        SaveProfileBasicDto.Request request = SaveProfileBasicDto.Request.builder()
                .name("홍길동").terms(List.of(10L, 20L)).build();
        given(basicInformationService.save(any(BasicInformation.class))).willReturn(mock(BasicInformation.class));
        given(agreementService.saveMemberAgreement(request.getTerms(), memberId)).willReturn(true);

        assertThatThrownBy(() -> onBoardingService.saveBasicInfoLogic(request, memberId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(OnBoardErrorCode.ONBOARD_TERM_SAVE_ERROR));

        verifyNoInteractions(memberPrivacyService);
    }

    @Test
    @DisplayName("회원이 없으면 기본 정보와 약관을 저장하지 않는다")
    void saveBasicInfoLogic_FailsBeforeAgreementSaveWhenMemberMissing() {
        SaveProfileBasicDto.Request request = SaveProfileBasicDto.Request.builder()
                .terms(List.of(10L)).build();
        given(memberService.findById(memberId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> onBoardingService.saveBasicInfoLogic(request, memberId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.MEMBER_NOT_FOUND));

        verifyNoInteractions(basicInformationService, agreementService);
    }

    @Test
    @DisplayName("라이프스타일 저장 성공 테스트")
    void saveLifeStyleLogic_Success() {
        SaveProfileLifeStyleDto.Request request = SaveProfileLifeStyleDto.Request.builder()
                .lifestyles(List.of(1L))
                .build();
 
        given(memberLifePatternService.saveMemberLifePatterns(memberId, request.getLifestyles()))
                .willReturn(List.of(10L));
 
        SaveProfileLifeStyleDto.Response response = onBoardingService.saveLifeStyleLogic(request, memberId);
 
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(memberLifePatternService).saveMemberLifePatterns(memberId, request.getLifestyles());
        verify(memberPrivacyService).save(argThat(privacy ->
                member.equals(privacy.getMember()) && privacy.getType() == MemberPrivacyType.PUBLIC));
    }
 
    @Test
    @DisplayName("방 정보 저장 성공 테스트 (OFFER)")
    void saveRoomInfoLogic_Offer_Success() {
        SaveProfileRoomInfoDto.Request request = SaveProfileRoomInfoDto.Request.builder()
                .type(RoomProfileType.OFFER)
                .region(List.of(1L))
                .roomProfile(List.of(1L))
                .build();
 
        given(metaService.findByRegionId(anyLong())).willReturn(Optional.of(mock(Region.class)));
        given(roomProfileService.save(any(RoomOfferProfile.class))).willReturn(mock(RoomOfferProfile.class));
        given(metaService.findByRoomTypes(any())).willReturn(List.of(mock(RoomType.class)));
 
        SaveProfileRoomInfoDto.Response response = onBoardingService.saveRoomInfoLogic(request, memberId);
 
        assertThat(response).isNotNull();
        verify(roomProfileService).save(any(RoomOfferProfile.class));
        verify(roomTypeService).saveOfferRoomTypeAll(any());
    }
 
    @Test
    @DisplayName("방 정보 저장 성공 테스트 (SEEKER)")
    void saveRoomInfoLogic_Seeker_Success() {
        SaveProfileRoomInfoDto.Request request = SaveProfileRoomInfoDto.Request.builder()
                .type(RoomProfileType.SEEKER)
                .region(List.of(1L))
                .roomProfile(List.of(1L))
                .build();
 
        given(roomProfileService.save(any(RoomSeekerProfile.class))).willReturn(mock(RoomSeekerProfile.class));
        given(metaService.findByRegions(any())).willReturn(List.of(mock(Region.class)));
        given(metaService.findByRoomTypes(any())).willReturn(List.of(mock(RoomType.class)));
 
        SaveProfileRoomInfoDto.Response response = onBoardingService.saveRoomInfoLogic(request, memberId);
 
        assertThat(response).isNotNull();
        verify(roomProfileService).save(any(RoomSeekerProfile.class));
        verify(roomSeekerProfileRegionService).saveAll(any());
        verify(roomTypeService).saveSeekerRoomTypeAll(any());
    }
 
    @Test
    @DisplayName("전체 프로필 저장 성공 테스트")
    void saveAll_Success() {
        SaveProfileAllDto.Request request = new SaveProfileAllDto.Request();
        request.setType(RoomProfileType.OFFER);
        request.setTerms(List.of(1L));
        request.setLifestyles(List.of(1L));
        request.setRegion(List.of(1L));
        request.setRoomProfile(List.of(1L));
 
        given(basicInformationService.save(any(BasicInformation.class))).willReturn(mock(BasicInformation.class));
        given(memberLifePatternService.saveMemberLifePatterns(memberId, request.getLifestyles()))
                .willReturn(List.of(10L));
        given(metaService.findByRegionId(anyLong())).willReturn(Optional.of(mock(Region.class)));
        given(roomProfileService.save(any(RoomOfferProfile.class))).willReturn(mock(RoomOfferProfile.class));
        given(metaService.findByRoomTypes(any())).willReturn(List.of(mock(RoomType.class)));
 
        SaveProfileAllDto.Response response = onBoardingService.saveAll(request, memberId);
 
        assertThat(response).isNotNull();
        verify(basicInformationService).save(any(BasicInformation.class));
        verify(memberLifePatternService).saveMemberLifePatterns(memberId, request.getLifestyles());
        verify(roomProfileService).save(any(RoomOfferProfile.class));
        verify(agreementService).saveMemberAgreement(request.getTerms(), memberId);
    }

    @Test
    @DisplayName("전체 프로필 저장 중 약관 저장에 실패하면 나머지 프로필을 저장하지 않는다")
    void saveAll_FailsBeforeLifeStyleAndRoomSaveWhenAgreementSaveFails() {
        SaveProfileAllDto.Request request = new SaveProfileAllDto.Request();
        request.setTerms(List.of(10L, 20L));
        given(basicInformationService.save(any(BasicInformation.class))).willReturn(mock(BasicInformation.class));
        given(agreementService.saveMemberAgreement(request.getTerms(), memberId)).willReturn(true);

        assertThatThrownBy(() -> onBoardingService.saveAll(request, memberId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(OnBoardErrorCode.ONBOARD_TERM_SAVE_ERROR));

        verifyNoInteractions(memberLifePatternService, roomProfileService, memberPrivacyService);
    }

    @Test
    @DisplayName("전체 프로필 수정은 약관 ID와 회원 ID를 agreement 서비스에 전달한다")
    void modifyAll_DelegatesTermsToAgreementService() {
        ModifyProfileAllDto.Request request = new ModifyProfileAllDto.Request();
        request.setTerms(List.of(10L, 20L));
        request.setLifestyles(List.of());
        request.setType(RoomProfileType.SEEKER);
        request.setRegion(List.of(100L));
        request.setRoomProfile(List.of(200L));
        BasicInformation basicInformation = mock(BasicInformation.class);
        RoomSeekerProfile roomProfile = mock(RoomSeekerProfile.class);
        given(basicInformationService.findByMember(member)).willReturn(List.of(basicInformation));
        given(roomProfileService.findByMember(member)).willReturn(List.of(roomProfile));
        given(roomProfile.getType()).willReturn(RoomProfileType.SEEKER);

        ModifyProfileAllDto.Response response = onBoardingService.modifyAll(request, memberId);

        assertThat(response.getUpdatedAt()).isNotNull();
        verify(agreementService).modifyMemberAgreement(request.getTerms(), memberId);
    }
 
    @Test
    @DisplayName("기본 정보 수정 성공 테스트")
    void modifyBasicInfoLogic_Success() {
        ModifyProfileBasicDto.Request request = ModifyProfileBasicDto.Request.builder()
                .name("이몽룡")
                .terms(List.of(1L))
                .build();

        BasicInformation basicInfo = mock(BasicInformation.class);
        given(basicInformationService.findByMember(member)).willReturn(List.of(basicInfo));

        ModifyProfileBasicDto.Response response = onBoardingService.modifyBasicInfoLogic(request, memberId, null);

        assertThat(response).isNotNull();
        verify(basicInfo).modifyBasicInformation(request);
        verify(agreementService).modifyMemberAgreement(request.getTerms(), memberId);
    }
 
    @Test
    @DisplayName("프로필 공개 여부 변경 성공 테스트")
    void changeProfileStatus_Success() {
        ProfileVisibilityDto.Request request = new ProfileVisibilityDto.Request();
        request.setStatus(MemberPrivacyType.PUBLIC);
 
        given(myRoomMateService.isExistRoomMate(member)).willReturn(false);
        MemberPrivacy privacy = mock(MemberPrivacy.class);
        given(memberPrivacyService.findByMember(member)).willReturn(List.of(privacy));
 
        ProfileVisibilityDto.Response response = onBoardingService.changeProfileStatus(request, memberId);
 
        assertThat(response).isNotNull();
        verify(privacy).changeState(MemberPrivacyType.PUBLIC);
    }
 
    @Test
    @DisplayName("룸메이트가 있을 때 프로필 상태 변경 시 예외 발생")
    void changeProfileStatus_Fail_WhenRoomMateExists() {
        ProfileVisibilityDto.Request request = new ProfileVisibilityDto.Request();
        given(myRoomMateService.isExistRoomMate(member)).willReturn(true);
 
        assertThatThrownBy(() -> onBoardingService.changeProfileStatus(request, memberId))
                .isInstanceOf(BusinessException.class);
    }
 
    @Test
    @DisplayName("라이프스타일 수정 성공 테스트")
    void modifyLifeStyleLogic_Success() {
        ModifyProfileLifeStyleDto.Request.LifeStyleInfo info = new ModifyProfileLifeStyleDto.Request.LifeStyleInfo();
        info.setId(1L);
        info.setLifestyleId(10L);
        ModifyProfileLifeStyleDto.Request request = ModifyProfileLifeStyleDto.Request.builder()
                .lifestyles(List.of(info))
                .build();
 
        ModifyProfileLifeStyleDto.Response response = onBoardingService.modifyLifeStyleLogic(request, memberId);
 
        assertThat(response.getUpdatedAt()).isNotNull();
        InOrder lifePatternCalls = inOrder(memberLifePatternService);
        lifePatternCalls.verify(memberLifePatternService).modifyLifeStyle(request, memberId);
        lifePatternCalls.verify(memberLifePatternService).modifyLifeStyleLog(memberId);
    }
 
    @Test
    @DisplayName("방 정보 수정 테스트 (OFFER -> SEEKER 전환)")
    void modifyRoomInfoLogic_Switch_Success() {
        ModifyProfileRoomInfoDto.Request request = ModifyProfileRoomInfoDto.Request.builder()
                .type(RoomProfileType.SEEKER)
                .region(List.of(1L))
                .roomProfile(List.of(1L))
                .build();
 
        RoomOfferProfile oldProfile = mock(RoomOfferProfile.class);
        given(oldProfile.getType()).willReturn(RoomProfileType.OFFER);
        given(roomProfileService.findByMember(member)).willReturn(List.of(oldProfile));
        given(roomProfileService.save(any(RoomSeekerProfile.class))).willReturn(mock(RoomSeekerProfile.class));
        given(metaService.findByRegions(any())).willReturn(List.of(mock(Region.class)));
        given(metaService.findByRoomTypes(any())).willReturn(List.of(mock(RoomType.class)));
 
        ModifyProfileRoomInfoDto.Response response = onBoardingService.modifyRoomInfoLogic(request, memberId);
 
        assertThat(response).isNotNull();
        verify(roomTypeService).deleteByRoomOfferProfile(oldProfile);
        verify(roomProfileService).delete(oldProfile);
        verify(roomProfileService).save(any(RoomSeekerProfile.class));
    }
 
    @Test
    @DisplayName("선호 조건 저장 성공 테스트")
    void savePreferenceConditionLogic_Success() {
        SavePreferencesConditionsDto.Request request = new SavePreferencesConditionsDto.Request(List.of(1L));
 
        given(preferenceConditionService.savePreferenceConditionWeights(memberId, request.getConditions()))
                .willReturn(List.of(10L));
        given(preferenceConditionService.findNextWeightLogDegreeNumber(memberId)).willReturn(2L);
        given(preferenceConditionService.savePreferenceConditionWeightLogDegree(2L)).willReturn(20L);
        given(preferenceConditionService.savePreferenceConditionWeightLogs(memberId, request.getConditions(), 20L))
                .willReturn(List.of(30L));
 
        SavePreferencesConditionsDto.Response response = onBoardingService.savePreferenceConditionLogic(request, memberId);
 
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(preferenceConditionService).savePreferenceConditionWeights(memberId, request.getConditions());
        verify(preferenceConditionService).savePreferenceConditionWeightLogs(memberId, request.getConditions(), 20L);
    }
 
    @Test
    @DisplayName("내가 쓴 게시글 조회 테스트")
    void findMyBoardList_Success() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<MyBoardListDto.Response.BoardItem> page = new PageImpl<>(List.of(mock(MyBoardListDto.Response.BoardItem.class)));
        given(roommateBoardService.getMyBoardList(any(), any())).willReturn(page);
 
        MyBoardListDto.Response response = onBoardingService.findMyBoardList(pageable, memberId);
 
        assertThat(response.getBoards()).hasSize(1);
    }

    @Test
    @DisplayName("선호 조건 저장 성공 테스트 (이미 존재할 시 삭제 후 재삽입)")
    void savePreferenceConditionLogic_Upsert_Success() {
        SavePreferencesConditionsDto.Request request = new SavePreferencesConditionsDto.Request(List.of(1L));

        given(preferenceConditionService.savePreferenceConditionWeights(memberId, request.getConditions()))
                .willReturn(List.of(10L));
        given(preferenceConditionService.findNextWeightLogDegreeNumber(memberId)).willReturn(2L);
        given(preferenceConditionService.savePreferenceConditionWeightLogDegree(2L)).willReturn(20L);
        given(preferenceConditionService.savePreferenceConditionWeightLogs(memberId, request.getConditions(), 20L))
                .willReturn(List.of(30L));

        SavePreferencesConditionsDto.Response response = onBoardingService.savePreferenceConditionLogic(request, memberId);

        assertThat(response.getUpdatedAt()).isNotNull();
        InOrder preferenceCalls = inOrder(preferenceConditionService);
        preferenceCalls.verify(preferenceConditionService).deletePreferenceConditionWeightByMemberId(memberId);
        preferenceCalls.verify(preferenceConditionService).savePreferenceConditionWeights(memberId, request.getConditions());
    }
}
