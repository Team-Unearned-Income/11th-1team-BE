package org.example.knockin.chat.event;

import java.util.Objects;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ChattingScoreQueryEvent {
    private final Long chattingRoomId;
    private final Long memberId;
    private Optional<Score> result;

    public void complete(Optional<Score> result) {
        this.result = Objects.requireNonNull(result);
    }

    public Optional<Score> getResult() {
        if (result == null) {
            throw new IllegalStateException("ChattingScoreQueryEvent was not handled synchronously");
        }
        return result;
    }

    public record Score(Long id, Integer score) {}
}
