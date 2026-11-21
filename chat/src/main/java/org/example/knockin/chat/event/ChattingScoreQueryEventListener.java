package org.example.knockin.chat.event;

import lombok.RequiredArgsConstructor;
import org.example.knockin.chat.entity.ChattingScore;
import org.example.knockin.chat.service.impl.ChattingScoreServiceImpl;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChattingScoreQueryEventListener {
    private final ChattingScoreServiceImpl chattingScoreService;

    @EventListener
    public void handle(ChattingScoreQueryEvent event) {
        event.complete(chattingScoreService
                .findByChattingRoomIdAndMemberId(event.getChattingRoomId(), event.getMemberId())
                .map(score -> new ChattingScoreQueryEvent.Score(score.getId(), score.getScore())));
    }

    @EventListener
    public void handle(ChattingScoreIdsQueryEvent event) {
        event.complete(chattingScoreService.findByChattingRoomId(event.getChattingRoomId()).stream()
                .map(ChattingScore::getId)
                .toList());
    }
}
