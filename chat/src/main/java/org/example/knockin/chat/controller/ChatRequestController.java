package org.example.knockin.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.knockin.chat.dto.ChatRequestDetailDto;
import org.example.knockin.chat.dto.ChatRequestDto;
import org.example.knockin.chat.dto.ChatRequestDto.Response;
import org.example.knockin.chat.dto.ChatRequestListDto;
import org.example.knockin.global.api.CommonResponse;
import org.example.knockin.global.auth.dto.PrincipalDetails;
import org.example.knockin.chat.service.impl.ChatRequestServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat-requests")
@Tag(name = "5. 채팅 요청")
public class ChatRequestController {
    private final ChatRequestServiceImpl chatRequestService;

    @GetMapping("")
    @Operation(summary = "채팅 요청 목록 조회", deprecated = true)
    public CommonResponse<List<ChatRequestListDto.Response>> findChatRequestList(
            @AuthenticationPrincipal PrincipalDetails details
    ) {
        Long memberId = details.getMember().getId();
        List<ChatRequestListDto.Response> pendingChatRequestList = chatRequestService.getPendingChatRequestList(memberId);
        return CommonResponse.status(HttpStatus.OK).body(pendingChatRequestList);
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "채팅 요청 상세 조회", deprecated = true)
    public CommonResponse<ChatRequestDetailDto.Response> findChatRequest(
            @AuthenticationPrincipal PrincipalDetails details,
            @PathVariable Long requestId
    ) {
        Long memberId = details.getMember().getId();
        ChatRequestDetailDto.Response response = chatRequestService.getChatRequestDetail(memberId, requestId);
        return CommonResponse.status(HttpStatus.OK).body(response);
    }

    @PostMapping("")
    @Operation(summary = "채팅 요청 저장", deprecated = true)
    public CommonResponse<ChatRequestDto.Response> saveChatRequest(
            @AuthenticationPrincipal PrincipalDetails details,
            @Valid @RequestBody ChatRequestDto.Request request
    ) {
        Long requesterId = details.getMember().getId();
        Response response = chatRequestService.saveChatRequest(requesterId, request);
        return CommonResponse.status(HttpStatus.OK).body(response);
    }


    @PostMapping("/{requestId}/accept")
    @Operation(summary = "채팅 요청 수락", deprecated = true)
    public CommonResponse<ChatRequestDto.Response> saveChatRequestAccept(
            @AuthenticationPrincipal PrincipalDetails details,
            @PathVariable Long requestId
    ) {
        Long memberId = details.getMember().getId();
        Response response = chatRequestService.acceptRequired(memberId, requestId);
        return CommonResponse.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{requestId}/reject")
    @Operation(summary = "채팅 요청 거절", deprecated = true)
    public CommonResponse<ChatRequestDto.Response> saveChatRequestReject(
            @AuthenticationPrincipal PrincipalDetails details,
            @PathVariable Long requestId
    ) {
        Long memberId = details.getMember().getId();
        Response response = chatRequestService.rejectRequired(memberId, requestId);
        return CommonResponse.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{requestId}/cancel")
    @Operation(summary = "채팅 요청 취소", deprecated = true)
    public CommonResponse<ChatRequestDto.Response> saveChatRequestCancel(
            @AuthenticationPrincipal PrincipalDetails details,
            @PathVariable Long requestId
    ) {
        Long memberId = details.getMember().getId();
        Response response = chatRequestService.cancelRequired(memberId, requestId);
        return CommonResponse.status(HttpStatus.OK).body(response);
    }
}

