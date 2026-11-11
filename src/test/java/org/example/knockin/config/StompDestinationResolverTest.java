package org.example.knockin.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.example.knockin.global.config.StompDestinationResolver;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.chat.exception.ChattingErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("STOMP destination 해석기")
class StompDestinationResolverTest {

    private final StompDestinationResolver resolver = new StompDestinationResolver();

    @Test
    @DisplayName("채팅방 구독 destination이면 채팅방 식별자를 반환한다")
    void resolveChatSubscribeRoomIdReturnsChatRoomId() {
        // When
        Optional<Long> chatRoomId = resolver.resolveChatSubscribeRoomId("/sub/chats/10");

        // Then
        assertThat(chatRoomId).contains(10L);
    }

    @Test
    @DisplayName("채팅방 구독 destination이 아니면 빈 값을 반환한다")
    void resolveChatSubscribeRoomIdReturnsEmptyForOtherDestination() {
        // When
        Optional<Long> chatRoomId = resolver.resolveChatSubscribeRoomId("/sub/alarms");

        // Then
        assertThat(chatRoomId).isEmpty();
    }

    @Test
    @DisplayName("채팅방 식별자를 숫자로 해석할 수 없으면 예외가 발생한다")
    void resolveChatSubscribeRoomIdRejectsNonNumericChatRoomId() {
        // When & Then
        assertThatThrownBy(() -> resolver.resolveChatSubscribeRoomId("/sub/chats/not-number"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ChattingErrorCode.ROOM_ACCESS_DENIED));
    }

    @Test
    @DisplayName("채팅방 구독 destination에 추가 경로가 있으면 빈 값을 반환한다")
    void resolveChatSubscribeRoomIdReturnsEmptyForNestedChatDestination() {
        // When
        Optional<Long> chatRoomId = resolver.resolveChatSubscribeRoomId("/sub/chats/10/typing");

        // Then
        assertThat(chatRoomId).isEmpty();
    }
}
