package org.example.knockin.mate.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.knockin.global.api.CommonResponse;
import org.example.knockin.global.auth.dto.PrincipalDetails;
import org.example.knockin.mate.dto.RoommateRequestDto;
import org.example.knockin.mate.dto.RoommateRequestDto.Response;
import org.example.knockin.mate.dto.RoommateRequestListDto;
import org.example.knockin.mate.service.impl.RoommateRequestServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/roommate-requests")
@Tag(name = "8. 룸메이트 관리")
public class RoomMateRequestController {
    private final RoommateRequestServiceImpl roommateRequestService;

    @PostMapping("")
    @Operation(summary = "룸메이트 요청 저장")
    public CommonResponse<RoommateRequestDto.Response> saveRoomMateRequest(
            @AuthenticationPrincipal PrincipalDetails details,
            @RequestBody RoommateRequestDto.Request request
    ) {
        Long memberId = details.getMember().getId();
        Response response = roommateRequestService.saveRoommateRequest(memberId, request);
        return CommonResponse.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{requestId}/accept")
    @Operation(summary = "룸메이트 요청 수락")
    public CommonResponse<RoommateRequestDto.Response> saveRoomMateRequestAccept(
            @AuthenticationPrincipal PrincipalDetails details,
            @PathVariable Long requestId
    ) {
        Long memberId = details.getMember().getId();
        Response response = roommateRequestService.acceptRequired(memberId, requestId);
        return CommonResponse.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{requestId}/reject")
    @Operation(summary = "룸메이트 요청 거절")
    public CommonResponse<RoommateRequestDto.Response> saveRoomMateRequestReject(
            @AuthenticationPrincipal PrincipalDetails details,
            @PathVariable Long requestId
    ) {
        Long memberId = details.getMember().getId();
        Response response = roommateRequestService.rejectRequired(memberId, requestId);
        return CommonResponse.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{requestId}/cancel")
    @Operation(summary = "룸메이트 요청 취소")
    public CommonResponse<RoommateRequestDto.Response> saveRoomMateRequestCancel(
            @AuthenticationPrincipal PrincipalDetails details,
            @PathVariable Long requestId
    ) {
        Long memberId = details.getMember().getId();
        Response response = roommateRequestService.cancelRequired(memberId, requestId);
        return CommonResponse.status(HttpStatus.OK).body(response);
    }

    @GetMapping("")
    @Operation(summary = "룸메이트 요청 목록 조회")
    public CommonResponse<Page<RoommateRequestListDto.Response>> findRoomMateRequestList(
            @AuthenticationPrincipal PrincipalDetails details,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long memberId = details.getMember().getId();
        Page<RoommateRequestListDto.Response> result = roommateRequestService.getRequiredList(memberId, pageable);
        return CommonResponse.status(HttpStatus.OK).body(result);
    }
}

