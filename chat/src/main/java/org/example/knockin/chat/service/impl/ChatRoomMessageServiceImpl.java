package org.example.knockin.chat.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.knockin.chat.dto.ChatRoomDetailDto;
import org.example.knockin.chat.dto.MessageType;
import org.example.knockin.chat.entity.ChatRoomMessage;
import org.example.knockin.chat.entity.ChattingRoom;
import org.example.knockin.chat.repository.ChatRoomMessageRepository;
import org.example.knockin.chat.repository.row.ChatRoomUnreadCountRow;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRoomMessageServiceImpl {

    private final ChatRoomMessageRepository chatRoomMessageRepository;

    public ChatRoomMessage save(String contents, @Nullable Long memberId, ChattingRoom chattingRoom, MessageType type) {
        ChatRoomMessage chatRoomMessage = ChatRoomMessage.builder()
                .contents(contents)
                .memberId(memberId)
                .chattingRoom(chattingRoom)
                .type(type)
                .build();

        return chatRoomMessageRepository.save(chatRoomMessage);
    }

    public List<ChatRoomDetailDto.ChatMessage> findChatMessageDto(Long chatRoomId) {
        return chatRoomMessageRepository.findChatMessageDto(chatRoomId);
    }

    public List<ChatRoomUnreadCountRow> findUnreadMessageCounts(Long memberId, List<Long> chatRoomIds) {
        return chatRoomMessageRepository.findUnreadMessageCounts(memberId, chatRoomIds);
    }

    public long markUnreadMessagesAsRead(Long chatRoomId, Long memberId) {
        return chatRoomMessageRepository.markUnreadMessagesAsRead(chatRoomId, memberId);
    }
}
