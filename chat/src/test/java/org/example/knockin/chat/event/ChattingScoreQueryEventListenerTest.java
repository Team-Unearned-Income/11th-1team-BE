package org.example.knockin.chat.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.example.knockin.chat.entity.ChattingScore;
import org.example.knockin.chat.service.impl.ChattingScoreServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class ChattingScoreQueryEventListenerTest {
    private ChattingScoreServiceImpl chattingScoreService;
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        chattingScoreService = mock(ChattingScoreServiceImpl.class);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(ChattingScoreServiceImpl.class, () -> chattingScoreService);
        context.register(ChattingScoreQueryEventListener.class);
        context.refresh();
    }

    @AfterEach
    void close() {
        context.close();
    }

    @Test
    void returnsScoreBeforePublishReturns() {
        Thread caller = Thread.currentThread();
        when(chattingScoreService.findByChattingRoomIdAndMemberId(100L, 1L)).thenAnswer(invocation -> {
            assertThat(Thread.currentThread()).isSameAs(caller);
            return Optional.of(ChattingScore.builder().id(203L).score(92).build());
        });
        ChattingScoreQueryEvent event = new ChattingScoreQueryEvent(100L, 1L);

        context.publishEvent(event);

        assertThat(event.getResult()).contains(new ChattingScoreQueryEvent.Score(203L, 92));
    }

    @Test
    void returnsEmptyWhenScoreDoesNotExist() {
        when(chattingScoreService.findByChattingRoomIdAndMemberId(100L, 1L)).thenReturn(Optional.empty());
        ChattingScoreQueryEvent event = new ChattingScoreQueryEvent(100L, 1L);

        context.publishEvent(event);

        assertThat(event.getResult()).isEmpty();
    }

    @Test
    void returnsBothScoreIdsInOrder() {
        when(chattingScoreService.findByChattingRoomId(100L)).thenReturn(List.of(
                ChattingScore.builder().id(201L).score(80).build(),
                ChattingScore.builder().id(202L).score(70).build()));
        ChattingScoreIdsQueryEvent event = new ChattingScoreIdsQueryEvent(100L);

        context.publishEvent(event);

        assertThat(event.getResult()).containsExactly(201L, 202L);
    }

    @Test
    void returnsEmptyIdsWhenRoomHasNoScores() {
        when(chattingScoreService.findByChattingRoomId(100L)).thenReturn(List.of());
        ChattingScoreIdsQueryEvent event = new ChattingScoreIdsQueryEvent(100L);

        context.publishEvent(event);

        assertThat(event.getResult()).isEmpty();
    }

    @Test
    void propagatesLookupFailureInsteadOfTreatingItAsMissingScore() {
        IllegalStateException failure = new IllegalStateException("lookup failed");
        when(chattingScoreService.findByChattingRoomIdAndMemberId(100L, 1L)).thenThrow(failure);

        assertThatThrownBy(() -> context.publishEvent(new ChattingScoreQueryEvent(100L, 1L)))
                .isSameAs(failure);
    }

    @Test
    void missingListenerIsNotAnEmptyResult() {
        try (AnnotationConfigApplicationContext emptyContext = new AnnotationConfigApplicationContext()) {
            emptyContext.refresh();
            ChattingScoreQueryEvent scoreEvent = new ChattingScoreQueryEvent(100L, 1L);
            ChattingScoreIdsQueryEvent idsEvent = new ChattingScoreIdsQueryEvent(100L);
            emptyContext.publishEvent(scoreEvent);
            emptyContext.publishEvent(idsEvent);

            assertThatThrownBy(scoreEvent::getResult).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(idsEvent::getResult).isInstanceOf(IllegalStateException.class);
        }
    }
}
