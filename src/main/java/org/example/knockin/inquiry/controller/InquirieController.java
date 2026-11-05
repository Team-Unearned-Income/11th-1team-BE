package org.example.knockin.inquiry.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.knockin.inquiry.dto.InquiryCategoryListDto;
import org.example.knockin.inquiry.dto.InquiryDetailDto;
import org.example.knockin.inquiry.dto.InquiryDto;
import org.example.knockin.inquiry.dto.InquiryListDto;
import org.example.knockin.global.api.CommonResponse;
import org.example.knockin.global.auth.dto.PrincipalDetails;
import org.example.knockin.inquiry.service.impl.InquiryServiceImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inquiries")
@RequiredArgsConstructor
@Tag(name = "9. 알림/고객센터")
public class InquirieController {
    private final InquiryServiceImpl inquirieService;

    @GetMapping("")
    @Operation(summary = "문의 목록 조회")
    public CommonResponse<InquiryListDto.Response> findInquirieList(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(inquirieService.findInquiryList(pageable, principalDetails.getMember().getId()));
    }

    @GetMapping("/{inquiryId}")
    @Operation(summary = "문의 상세 조회")
    public CommonResponse<InquiryDetailDto.Response> findInquirie(@PathVariable Long inquiryId, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(inquirieService.findInquiry(inquiryId, principalDetails.getMember().getId()));
    }

    @GetMapping("/categorys")
    @Operation(summary = "문의 카테고리 목록 조회")
    public CommonResponse<InquiryCategoryListDto.Response> findInquirieCategoryList() {
        return CommonResponse.status(HttpStatus.OK).body(inquirieService.findInquiryCategoryList());
    }

    @PostMapping("")
    @Operation(summary = "문의 저장")
    public CommonResponse<InquiryDto.Response> saveInquiry(@RequestBody InquiryDto.Request request, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(inquirieService.saveInquiryLogic(request, principalDetails.getMember().getId()));
    }
}

