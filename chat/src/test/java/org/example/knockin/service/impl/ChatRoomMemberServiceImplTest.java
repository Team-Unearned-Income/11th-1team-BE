package org.example.knockin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.example.knockin.chat.service.impl.ChatRoomMemberServiceImpl;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.chat.exception.ChattingErrorCode;
import org.example.knockin.chat.repository.ChatRoomMemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("채팅방 멤버 서비스")
class ChatRoomMemberServiceImplTest {

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @InjectMocks
    private ChatRoomMemberServiceImpl chatRoomMemberService;

    @Test
    @DisplayName("활성 채팅방 멤버는 채팅방 구독을 허용한다")
    void checkCanSubscribeAllowsActiveRoomMember() {
        // Given
        Long chatRoomId = 10L;
        Long memberId = 1L;
        when(chatRoomMemberRepository.existsActiveMember(chatRoomId, memberId)).thenReturn(true);

        // When
        chatRoomMemberService.checkCanSubscribe(chatRoomId, memberId);

        // Then
        verify(chatRoomMemberRepository).existsActiveMember(chatRoomId, memberId);
    }

    @Test
    @DisplayName("활성 채팅방 멤버가 아니면 채팅방 구독을 거부한다")
    void checkCanSubscribeRejectsInactiveRoomMember() {
        // Given
        Long chatRoomId = 10L;
        Long memberId = 1L;
        when(chatRoomMemberRepository.existsActiveMember(chatRoomId, memberId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> chatRoomMemberService.checkCanSubscribe(chatRoomId, memberId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ChattingErrorCode.ROOM_ACCESS_DENIED));
    }

    @Test
    @DisplayName("활성 채팅방 멤버는 메시지 전송을 허용한다")
    void checkCanSendMessageAllowsActiveRoomMember() {
        // Given
        Long chatRoomId = 10L;
        Long memberId = 1L;
        when(chatRoomMemberRepository.existsActiveMember(chatRoomId, memberId)).thenReturn(true);

        // When
        chatRoomMemberService.checkCanSendMessage(chatRoomId, memberId);

        // Then
        verify(chatRoomMemberRepository).existsActiveMember(chatRoomId, memberId);
    }

    @Test
    @DisplayName("활성 채팅방 멤버가 아니면 메시지 전송을 거부한다")
    void checkCanSendMessageRejectsInactiveRoomMember() {
        // Given
        Long chatRoomId = 10L;
        Long memberId = 1L;
        when(chatRoomMemberRepository.existsActiveMember(chatRoomId, memberId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> chatRoomMemberService.checkCanSendMessage(chatRoomId, memberId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ChattingErrorCode.ROOM_MEMBER_NOT_FOUND));
    }
}
