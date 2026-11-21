package org.example.knockin.chat.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RoommateRequestMessageEvent<T> {
    private final Long chatRoomId;
    private final T response;
    private boolean completed;

    public void complete() {
        completed = true;
    }

    public void requireCompleted() {
        if (!completed) {
            throw new IllegalStateException("RoommateRequestMessageEvent was not handled synchronously");
        }
    }
}
