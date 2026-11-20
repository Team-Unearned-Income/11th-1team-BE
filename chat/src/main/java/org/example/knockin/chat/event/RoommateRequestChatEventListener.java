package org.example.knockin.chat.event;

import lombok.RequiredArgsConstructor;
import org.example.knockin.chat.dto.ChatSocketResponse;
import org.example.knockin.chat.dto.EventType;
import org.example.knockin.chat.entity.ChatRoomMember;
import org.example.knockin.chat.service.impl.ChatRoomMemberServiceImpl;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoommateRequestChatEventListener {
    private final ChatRoomMemberServiceImpl chatRoomMemberService;
    private final SimpMessageSendingOperations messagingTemplate;

    @EventListener
    public void handle(ChatRoomPartnerQueryEvent event) {
        ChatRoomMember me = chatRoomMemberService.findActiveMemberByRoomIdAndMemberId(event.getChatRoomId(), event.getMemberId());
        event.complete(chatRoomMemberService.findPartnerMemberId(me, me.getChattingRoom().getId()));
    }

    @EventListener
    public void handle(RoommateRequestMessageEvent<?> event) {
        messagingTemplate.convertAndSend("/sub/chats/" + event.getChatRoomId(),
                ChatSocketResponse.of(EventType.ROOMMATE_REQUEST, event.getChatRoomId(), event.getResponse()));
        event.complete();
    }
}
