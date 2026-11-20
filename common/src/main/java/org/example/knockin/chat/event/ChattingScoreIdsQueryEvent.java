package org.example.knockin.chat.event;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ChattingScoreIdsQueryEvent {
    private final Long chattingRoomId;
    private List<Long> result;

    public void complete(List<Long> result) {
        this.result = List.copyOf(result);
    }

    public List<Long> getResult() {
        if (result == null) {
            throw new IllegalStateException("ChattingScoreIdsQueryEvent was not handled synchronously");
        }
        return result;
    }
}
