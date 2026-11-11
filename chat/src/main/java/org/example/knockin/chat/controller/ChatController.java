package org.example.knockin.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.knockin.chat.dto.ChatMessageDto;
import org.example.knockin.chat.dto.ChatRoomCreateDto;
import org.example.knockin.chat.dto.ChatRoomDto;
import org.example.knockin.chat.dto.ChatRoomImageDto;
import org.example.knockin.chat.dto.ChatRoomDetailDto;
import org.example.knockin.chat.dto.ChatRoomListDto;
import org.example.knockin.chat.dto.ChatRoomListDto.Response;
import org.example.knockin.global.api.CommonResponse;
import org.example.knockin.global.auth.dto.PrincipalDetails;
import org.example.knockin.global.auth.util.PrincipalMemberResolver;
import org.example.knockin.chat.service.impl.ChatServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
@Tag(name = "6. 채팅 (Chat)")
public class ChatController {
    private final ChatServiceImpl chatService;
    private final PrincipalMemberResolver principalMemberResolver;

    @GetMapping("")
    @Operation(summary = "채팅방 목록 조회")
    public CommonResponse<List<ChatRoomListDto.Response>> findChatRoomList(
            @AuthenticationPrincipal PrincipalDetails details
    ) {
        Long memberId = details.getMember().getId();
        List<Response> responses = chatService.getChatRoomList(memberId);
        return CommonResponse.status(HttpStatus.OK).body(responses);
    }

    @GetMapping("/{chatRoomId}")
    @Operation(summary = "채팅방 상세 조회")
    public CommonResponse<ChatRoomDetailDto.Response> findChatRoomDetail(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal PrincipalDetails details
    ) {
        Long memberId = details.getMember().getId();
        ChatRoomDetailDto.Response response = chatService.getChatRoomDetail(chatRoomId, memberId);
        return CommonResponse.status(HttpStatus.OK).body(response);
    }

    @PostMapping("")
    @Operation(summary = "채팅방 생성")
    public CommonResponse<ChatRoomCreateDto.Response> createChatRoom(
            @AuthenticationPrincipal PrincipalDetails details,
            @Valid @RequestBody ChatRoomCreateDto.Request request
    ) {
        Long memberId = details.getMember().getId();
        ChatRoomCreateDto.Response response = chatService.createChattingRoom(memberId, request);
        return CommonResponse.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{chatRoomId}/leave")
    @Operation(summary = "채팅방 나가기")
    public CommonResponse<ChatRoomDto.Response> leaveChatRoom(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal PrincipalDetails details
    ) {
        Long memberId = details.getMember().getId();
        ChatRoomDto.Response response = chatService.leaveChatRoom(memberId, chatRoomId);
        return CommonResponse.status(HttpStatus.OK).body(response);
    }

    @PostMapping(value = "/{chatRoomId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "채팅방 이미지 업로드")
    public CommonResponse<ChatRoomImageDto.Response> uploadImage(
            @PathVariable Long chatRoomId,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal PrincipalDetails details
    ) {
        Long memberId = details.getMember().getId();
        ChatRoomImageDto.Response response = chatService.uploadImage(chatRoomId, memberId, file);
        return CommonResponse.status(HttpStatus.OK).body(response);
    }

    @MessageMapping("/chats/{chatRoomId}/messages")
    @Operation(summary = "메시지 전송")
    public void sendMessage(
            @DestinationVariable("chatRoomId") Long chatRoomId,
            @Payload ChatMessageDto.Request request,
            Principal principal
    ) {
        Long memberId = principalMemberResolver.resolveMemberId(principal);
        chatService.sendUserMessage(chatRoomId, request, memberId);
    }
}

