package org.example.knockin.alarm.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RoommateRequestAlarmEvent {
    private final Long receiverId;
    private final Long requestId;
    private final String title;
    private final String contents;
    private final String deepLink;
    private boolean completed;

    public void complete() {
        completed = true;
    }

    public void requireCompleted() {
        if (!completed) {
            throw new IllegalStateException("RoommateRequestAlarmEvent was not handled synchronously");
        }
    }
}
