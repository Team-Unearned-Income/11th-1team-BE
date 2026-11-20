package org.example.knockin.declaration.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MemberReportRequestedEvent {
    private final Long reporterId;
    private final Long reportedId;
    private final String reason;
    private boolean completed;

    public void complete() {
        completed = true;
    }

    public void requireCompleted() {
        if (!completed) {
            throw new IllegalStateException("MemberReportRequestedEvent was not handled synchronously");
        }
    }
}
