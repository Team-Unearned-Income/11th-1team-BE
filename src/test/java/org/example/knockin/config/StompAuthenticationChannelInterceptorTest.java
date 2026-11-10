package org.example.knockin.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import org.example.knockin.verification.entity.LoginProviderType;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberRole;
import org.example.knockin.global.auth.dto.PrincipalDetails;
import org.example.knockin.global.auth.util.PrincipalMemberResolver;
import org.example.knockin.global.auth.util.TokenProvider;
import org.example.knockin.global.config.StompAuthenticationChannelInterceptor;
import org.example.knockin.global.config.StompDestinationResolver;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.chat.exception.ChattingErrorCode;
import org.example.knockin.chat.service.impl.ChatRoomMemberServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
@DisplayName("STOMP 인증 채널 인터셉터")
class StompAuthenticationChannelInterceptorTest {

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private ChatRoomMemberServiceImpl chatRoomMemberService;

    @Mock
    private StompDestinationResolver stompDestinationResolver;

    @Mock
    private PrincipalMemberResolver principalMemberResolver;

    @InjectMocks
    private StompAuthenticationChannelInterceptor interceptor;

    @Test
    @DisplayName("활성 채팅방 멤버가 채팅방을 구독하면 구독을 허용한다")
    void preSendAllowsActiveMemberToSubscribeChatRoom() {
        // Given
        Long chatRoomId = 10L;
        Long memberId = 1L;
        String destination = "/sub/chats/10";
        Authentication authentication = authentication(memberId);
        Message<byte[]> message = subscribeMessage(destination, authentication);
        when(stompDestinationResolver.resolveChatSubscribeRoomId(destination)).thenReturn(Optional.of(chatRoomId));
        when(principalMemberResolver.resolveMemberId(authentication)).thenReturn(memberId);
        // When
        Message<?> result = interceptor.preSend(message, null);

        // Then
        verify(chatRoomMemberService).checkCanSubscribe(chatRoomId, memberId);
        assertThat(result).isSameAs(message);
    }

    @Test
    @DisplayName("활성 채팅방 멤버가 아니면 채팅방 구독을 거부한다")
    void preSendRejectsSubscribeWhenMemberIsNotActiveChatRoomMember() {
        // Given
        Long chatRoomId = 10L;
        Long memberId = 1L;
        String destination = "/sub/chats/10";
        Authentication authentication = authentication(memberId);
        Message<byte[]> message = subscribeMessage(destination, authentication);
        when(stompDestinationResolver.resolveChatSubscribeRoomId(destination)).thenReturn(Optional.of(chatRoomId));
        when(principalMemberResolver.resolveMemberId(authentication)).thenReturn(memberId);
        doThrow(new BusinessException(ChattingErrorCode.ROOM_ACCESS_DENIED))
                .when(chatRoomMemberService)
                .checkCanSubscribe(chatRoomId, memberId);

        // When & Then
        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ChattingErrorCode.ROOM_ACCESS_DENIED));
    }

    @Test
    @DisplayName("채팅방 구독 경로가 아니면 채팅방 멤버 검증을 수행하지 않는다")
    void preSendSkipsChatRoomMemberCheckForOtherSubscriptionDestination() {
        // Given
        String destination = "/sub/alarms";
        Message<byte[]> message = subscribeMessage(destination, authentication(1L));
        when(stompDestinationResolver.resolveChatSubscribeRoomId(destination)).thenReturn(Optional.empty());

        // When
        Message<?> result = interceptor.preSend(message, null);

        // Then
        verifyNoInteractions(chatRoomMemberService, principalMemberResolver);
        assertThat(result).isSameAs(message);
    }

    private Message<byte[]> subscribeMessage(String destination, Authentication authentication) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(authentication);
        accessor.setSessionAttributes(Map.of(
                StompAuthenticationChannelInterceptor.TOKEN_EXPIRES_AT_SESSION_KEY,
                new Date(System.currentTimeMillis() + 60_000)
        ));
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Authentication authentication(Long memberId) {
        Member member = Member.builder()
                .id(memberId)
                .providerType(LoginProviderType.KAKAO)
                .providerId("provider-id")
                .role(MemberRole.USER)
                .build();
        PrincipalDetails details = new PrincipalDetails(member);
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }
}
