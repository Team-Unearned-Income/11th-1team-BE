package org.example.knockin.chat.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.knockin.chat.entity.ChatRoomFile;
import org.example.knockin.chat.entity.ChatRoomMessage;
import org.example.knockin.chat.repository.ChatRoomFileRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRoomFileServiceImpl {

    private final ChatRoomFileRepository chatRoomFileRepository;

    public ChatRoomFile save(Long fileId, ChatRoomMessage chatRoomMessage) {
        ChatRoomFile chatRoomFile = ChatRoomFile.builder()
                .fileId(fileId)
                .chatRoomMessage(chatRoomMessage)
                .build();

        return chatRoomFileRepository.save(chatRoomFile);
    }
}
