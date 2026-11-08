package org.example.knockin.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.knockin.verification.dto.MyVerificationListDto;
import org.example.knockin.member.dto.DeleteUserDto;
import org.example.knockin.member.dto.FcmDto;
import org.example.knockin.member.dto.LogOutDto;
import org.example.knockin.member.dto.ModifyProfileAllDto;
import org.example.knockin.member.dto.ModifyProfileBasicDto;
import org.example.knockin.member.dto.MyAccountDto;
import org.example.knockin.member.dto.ProfileVisibilityDto;
import org.example.knockin.member.dto.SaveProfileAllDto;
import org.example.knockin.member.dto.SaveProfileBasicDto;
import org.example.knockin.alarm.dto.AlarmSettingDto;
import org.example.knockin.alarm.dto.MyNotificationSettingsDto;
import org.example.knockin.board.dto.MyBoardListDto;
import org.example.knockin.member.dto.FcmDto.Response;
import org.example.knockin.global.api.CommonResponse;
import org.example.knockin.global.auth.dto.PrincipalDetails;
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
import org.example.knockin.meta.dto.NoticeDetailDto;
import org.example.knockin.meta.dto.NoticeListDto;
import org.example.knockin.room.dto.ModifyProfileRoomInfoDto;
import org.example.knockin.room.dto.SaveProfileRoomInfoDto;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.example.knockin.alarm.service.impl.NotificationServiceImpl;
import org.example.knockin.alarm.service.impl.NotificationSettingServiceImpl;
import org.example.knockin.util.service.impl.OnBoardingServiceImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
@Tag(name = "2. 온보딩/프로필")
public class UserController {
    private final MemberServiceImpl memberService;
    private final OnBoardingServiceImpl onBoardingService;
    private final NotificationSettingServiceImpl notificationSettingService;
    private final NotificationServiceImpl notificationService;

    @DeleteMapping
    @Operation(summary = "회원 탈퇴")
    public CommonResponse<DeleteUserDto.Response> deleteUser(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(memberService.deleteMember(principalDetails.getMember().getProviderId(), principalDetails.getMember().getProviderType()));
    }

    @PostMapping("/profile/basic")
    @Operation(summary = "기본정보 저장")
    public CommonResponse<SaveProfileBasicDto.Response> saveBasicInfo(@Valid @RequestBody SaveProfileBasicDto.Request request, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(onBoardingService.saveBasicInfoLogic(request, principalDetails.getMember().getId()));
    }

    @PostMapping("/profile/lifestyle")
    @Operation(summary = "라이프스타일 저장")
    public CommonResponse<SaveProfileLifeStyleDto.Response> saveLifeStyle(@Valid @RequestBody SaveProfileLifeStyleDto.Request request, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(onBoardingService.saveLifeStyleLogic(request, principalDetails.getMember().getId()));
    }

    @PostMapping("/profile/roominfo")
    @Operation(summary = "방 정보 저장")
    public CommonResponse<SaveProfileRoomInfoDto.Response> saveRoomInfo(@Valid @RequestBody SaveProfileRoomInfoDto.Request request, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(onBoardingService.saveRoomInfoLogic(request, principalDetails.getMember().getId()));
    }

    @PostMapping("/profile/all")
    @Operation(summary = "전체 정보 저장")
    public CommonResponse<SaveProfileAllDto.Response> saveAll(@Valid @RequestBody SaveProfileAllDto.Request request, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(onBoardingService.saveAll(request, principalDetails.getMember().getId()));
    }

    @PutMapping(value = "/profile/basic", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "기본정보 수정")
    public CommonResponse<ModifyProfileBasicDto.Response> modifyBasicInfo(@Valid @RequestPart(value = "request") ModifyProfileBasicDto.Request request, @RequestPart(value = "file", required = false) MultipartFile file, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(onBoardingService.modifyBasicInfoLogic(request, principalDetails.getMember().getId(), file));
    }

    @PutMapping("/profile/lifestyle")
    @Operation(summary = "라이프스타일 수정")
    public CommonResponse<ModifyProfileLifeStyleDto.Response> modifyLifeStyle(@Valid @RequestBody ModifyProfileLifeStyleDto.Request request, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(onBoardingService.modifyLifeStyleLogic(request, principalDetails.getMember().getId()));
    }

    @PutMapping("/profile/roominfo")
    @Operation(summary = "방 정보 수정")
    public CommonResponse<ModifyProfileRoomInfoDto.Response> modifyRoomInfo(@Valid @RequestBody ModifyProfileRoomInfoDto.Request request, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(onBoardingService.modifyRoomInfoLogic(request, principalDetails.getMember().getId()));
    }

    @PutMapping("/profile/all")
    @Operation(summary = "전체 정보 수정")
    public CommonResponse<ModifyProfileAllDto.Response> modifyAll(@Valid @RequestBody ModifyProfileAllDto.Request request, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(onBoardingService.modifyAll(request, principalDetails.getMember().getId()));
    }

    @PostMapping("/preferences/lifestyle")
    @Operation(summary = "선호 라이프스타일 저장")
    public CommonResponse<SavePreferencesLifeStyleDto.Response> savePreLifeStyle(@RequestBody SavePreferencesLifeStyleDto.Request request, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(onBoardingService.savePreferenceLifeStyleLogic(request, principalDetails.getMember().getId()));
    }

    @PostMapping("/preferences/conditions")
    @Operation(summary = "선호 조건 저장")
    public CommonResponse<SavePreferencesConditionsDto.Response> savePreConditions(@RequestBody SavePreferencesConditionsDto.Request request, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(onBoardingService.savePreferenceConditionLogic(request, principalDetails.getMember().getId()));
    }

    @PostMapping("/preferences/all")
    @Operation(summary = "선호 전체 저장")
    public CommonResponse<SavePreferencesAllDto.Response> savePreAll(@RequestBody SavePreferencesAllDto.Request request, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(onBoardingService.savePreferenceAll(request, principalDetails.getMember().getId()));
    }

    @PutMapping("/preferences/lifestyle")
    @Operation(summary = "선호 라이프스타일 수정")
    public CommonResponse<ModifyPreferencesLifeStyleDto.Response> modifyPreLifeStyle(@RequestBody ModifyPreferencesLifeStyleDto.Request request, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(onBoardingService.modifyPreLifeStyleLogic(request, principalDetails.getMember().getId()));
    }

    @PutMapping("/preferences/conditions")
    @Operation(summary = "선호 조건 수정")
    public CommonResponse<ModifyPreferencesConditionsDto.Response> modifyPreConditions(@RequestBody ModifyPreferencesConditionsDto.Request request, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(onBoardingService.modifyPreConditionLogic(request, principalDetails.getMember().getId()));
    }

    @PutMapping("/preferences/all")
    @Operation(summary = "선호 전체 수정")
    public CommonResponse<ModifyPreferencesAllDto.Response> modifyPreAll(@RequestBody ModifyPreferencesAllDto.Request request, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(onBoardingService.modifyPreAll(request, principalDetails.getMember().getId()));
    }

    @GetMapping("/preferences/all")
    @Operation(summary = "선호 전체 조회")
    public CommonResponse<MyPreferencesAllDto.Response> findPreAll(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(onBoardingService.findPreAll(principalDetails.getMember().getId()));
    }

    @GetMapping("/profile")
    @Operation(summary = "내 프로필 정보 조회")
    public CommonResponse<MyProfileAllDto.Response.UserInfo> findProfileInfo(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(onBoardingService.findProfileInfo(principalDetails.getMember().getId()));
    }

    @GetMapping("/profile/all")
    @Operation(summary = "내 프로필 전체 조회")
    public CommonResponse<MyProfileAllDto.Response> findProfileAll(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(onBoardingService.findProfileAll(principalDetails.getMember().getId()));
    }

    @PatchMapping("/visibility")
    @Operation(summary = "프로필 공개 여부 변경")
    public CommonResponse<ProfileVisibilityDto.Response> changeProfileStatus(@RequestBody ProfileVisibilityDto.Request request, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(onBoardingService.changeProfileStatus(request, principalDetails.getMember().getId()));
    }

    @GetMapping("/boards")
    @Operation(summary = "내가 쓴 게시글 조회")
    public CommonResponse<MyBoardListDto.Response> findMyBoardList(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(onBoardingService.findMyBoardList(pageable, principalDetails.getMember().getId()));
    }

    @GetMapping("/verifications")
    @Operation(summary = "내 인증 현황 조회")
    public CommonResponse<MyVerificationListDto.Response> findVerificationList(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(onBoardingService.findVerificationList(pageable, principalDetails.getMember().getId()));
    }

    @GetMapping("/notification-settings")
    @Operation(summary = "알림 설정 조회")
    public CommonResponse<MyNotificationSettingsDto.Response> findAlaramSettingList(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(notificationSettingService.findAlaramSettingList(principalDetails.getMember().getId()));
    }

    @PatchMapping("/notification-settings")
    @Operation(summary = "알림 설정 수정")
    public CommonResponse<AlarmSettingDto.Response> modifyAlarmSetting(@RequestBody AlarmSettingDto.Request request, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(notificationSettingService.modifyAlarmSetting(request, principalDetails.getMember().getId()));
    }

    @GetMapping("/notices")
    @Operation(summary = "공지사항 목록 조회")
    public CommonResponse<NoticeListDto.Response> findNoticeList(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return CommonResponse.status(HttpStatus.OK).body(notificationService.findNoticeList(pageable));
    }

    @GetMapping("/notices/{id}")
    @Operation(summary = "공지사항 상세 조회")
    public CommonResponse<NoticeDetailDto.Response> findNotice(@PathVariable Long id) {
        return CommonResponse.status(HttpStatus.OK).body(notificationService.findNotification(id));
    }

    @PostMapping("/devices")
    @Operation(summary = "FCM 디바이스 정보 저장 (로그인 직후 호출)")
    public CommonResponse<FcmDto.Response> upsertDeviceProps(
            @AuthenticationPrincipal PrincipalDetails details,
            @Valid @RequestBody FcmDto.Request request
    ) {
        Response response = memberService.upsertFcmProps(details.getMember().getId(), request);
        return CommonResponse.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃")
    public CommonResponse<LogOutDto.Response> logout(@AuthenticationPrincipal PrincipalDetails details) {
        LogOutDto.Response response = memberService.logout(details.getMember().getId());
        return CommonResponse.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/account")
    @Operation(summary = "내 계정 권한 및 로그인 제공자 조회")
    public CommonResponse<MyAccountDto.Response> findMyAccount(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(memberService.findMyAccountRole(principalDetails.getMember().getId()));
    }
}
