package org.example.knockin.chat.event;

import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ChatRoomPartnerQueryEvent {
    private final Long chatRoomId;
    private final Long memberId;
    private Long result;

    public void complete(Long partnerId) {
        this.result = Objects.requireNonNull(partnerId);
    }

    public Long getResult() {
        if (result == null) {
            throw new IllegalStateException("ChatRoomPartnerQueryEvent was not handled synchronously");
        }
        return result;
    }
}
