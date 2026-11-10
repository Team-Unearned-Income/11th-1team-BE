package org.example.knockin.global.config;

import java.util.Optional;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.chat.exception.ChattingErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

@Component
public class StompDestinationResolver {
    private static final PathPattern CHAT_SUBSCRIBE_PATTERN = new PathPatternParser().parse("/sub/chats/{chatRoomId}");

    public Optional<Long> resolveChatSubscribeRoomId(String destination) {
        if (!StringUtils.hasText(destination)) {
            return Optional.empty();
        }

        PathContainer path = PathContainer.parsePath(destination);
        PathPattern.PathMatchInfo matchInfo = CHAT_SUBSCRIBE_PATTERN.matchAndExtract(path);
        if (matchInfo == null) {
            return Optional.empty();
        }

        String chatRoomId = matchInfo.getUriVariables().get("chatRoomId");
        try {
            return Optional.of(Long.valueOf(chatRoomId));
        } catch (NumberFormatException e) {
            throw new BusinessException(ChattingErrorCode.ROOM_ACCESS_DENIED);
        }
    }
}
