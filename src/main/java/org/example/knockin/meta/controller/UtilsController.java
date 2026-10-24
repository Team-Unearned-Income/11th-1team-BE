package org.example.knockin.meta.controller;

import jakarta.validation.Valid;
import org.example.knockin.global.auth.dto.PrincipalDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.knockin.agreement.dto.TermsDetailDto;
import org.example.knockin.agreement.dto.TermsListDto;
import org.example.knockin.global.api.CommonResponse;
import org.example.knockin.life.dto.MetaLifestylePatternsDto;
import org.example.knockin.meta.dto.*;
import org.example.knockin.room.dto.MetaRoomAddOptionsDto;
import org.example.knockin.room.dto.MetaRoomTypesDto;
import org.example.knockin.meta.service.impl.AppVersionServiceImpl;
import org.example.knockin.meta.service.impl.AuthEmailServiceImpl;
import org.example.knockin.meta.service.impl.FaqServiceImpl;
import org.example.knockin.mate.service.impl.MetaServiceImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "3. 일반/메타데이터")
public class UtilsController {
    private final MetaServiceImpl metaService;
    private final FaqServiceImpl faqService;
    private final AppVersionServiceImpl appVersionService;
    private final AuthEmailServiceImpl authEmailService;

    @GetMapping("/terms")
    @Operation(summary = "약관 목록 조회")
    public CommonResponse<TermsListDto.Response> findTermList() {
        return CommonResponse.status(HttpStatus.OK).body(metaService.findTermList());
    }

    @GetMapping("/terms/{termsId}")
    @Operation(summary = "약관 상세 조회")
    public CommonResponse<TermsDetailDto.Response> findTerm(@PathVariable Long termsId) {
        return CommonResponse.status(HttpStatus.OK).body(metaService.findTermDetail(termsId));
    }

    @GetMapping("/search/popular")
    @Operation(summary = "인기 검색어 조회")
    public CommonResponse<PopularSearchDto.Response> findPopSearch() {
        return CommonResponse.status(HttpStatus.OK).body(metaService.findPopSearch());
    }

    @GetMapping("/meta/lifestyle-patterns")
    @Operation(summary = "라이프스타일 패턴 메타데이터 조회")
    public CommonResponse<MetaLifestylePatternsDto.Response> findLifeStylePatterns() {
        return CommonResponse.status(HttpStatus.OK).body(metaService.findLifeStylePatterns());
    }

    @GetMapping("/meta/room-types")
    @Operation(summary = "방 유형 메타데이터 조회")
    public CommonResponse<MetaRoomTypesDto.Response> findRoomTypes() {
        return CommonResponse.status(HttpStatus.OK).body(metaService.findRoomTypes());
    }

    @GetMapping("/meta/regions")
    @Operation(summary = "지역 메타데이터 조회")
    public CommonResponse<MetaRegionsDto.Response> findRegions() {
        return CommonResponse.status(HttpStatus.OK).body(metaService.findRegions());
    }

    @GetMapping("/meta/room-add-options")
    @Operation(summary = "방 추가 옵션 메타데이터 조회")
    public CommonResponse<MetaRoomAddOptionsDto.Response> findRoomAddOptions() {
        return CommonResponse.status(HttpStatus.OK).body(metaService.findRoomAddOptions());
    }

    @GetMapping("/meta/faq")
    @Operation(summary = "자주묻는 질문 목록 조회")
    public CommonResponse<FaqListDto.Response> findFaqList(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return CommonResponse.status(HttpStatus.OK).body(faqService.findFaqList(pageable));
    }

    @GetMapping("/meta/faqAll")
    @Operation(summary = "자주묻는 질문 목록,상세 조회")
    public CommonResponse<FaqAllListDto.Response> findFaqAllList(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return CommonResponse.status(HttpStatus.OK).body(faqService.findFaqAllList(pageable));
    }

    @GetMapping("/meta/faq/{id}")
    @Operation(summary = "자주묻는 질문 상세 조회")
    public CommonResponse<FaqDto.Response> findFaq(@PathVariable Long id) {
        return CommonResponse.status(HttpStatus.OK).body(faqService.findFaq(id));
    }

    @GetMapping("/meta/app-version/android")
    @Operation(summary = "현재 앱버전 android 조회")
    public CommonResponse<AppVersionDto.Response> findAppAndroidVersionLatest() {
        return CommonResponse.status(HttpStatus.OK).body(appVersionService.findAppAndroidVersionLatest());
    }

    @GetMapping("/meta/app-version/ios")
    @Operation(summary = "현재 앱버전 ios 조회")
    public CommonResponse<AppVersionDto.Response> findAppIosVersionLatest() {
        return CommonResponse.status(HttpStatus.OK).body(appVersionService.findAppIosVersionLatest());
    }

    @GetMapping("/meta/auth-email")
    @Operation(summary = "인증 이메일 목록 조회")
    public CommonResponse<AuthEmailListDto.Response> findAuthEmailList() {
        return CommonResponse.status(HttpStatus.OK).body(authEmailService.findAuthEmailList());
    }

    @GetMapping("/auth/success")
    public CommonResponse<String> authSuccess() {
        return CommonResponse.status(HttpStatus.OK).body("로그인에 성공하였습니다.");
    }

    @PostMapping("/presigned-url")
    @Operation(summary = "R2 PreSigned Url 호출")
    public CommonResponse<PresignedUrlDto.Response> presignedUploadUrl(@Valid @RequestBody PresignedUrlDto.Request request, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        Long memberId = principalDetails.getMember().getId();
        return CommonResponse.status(HttpStatus.OK).body(metaService.presignedUploadUrl(request, memberId));
    }
}

