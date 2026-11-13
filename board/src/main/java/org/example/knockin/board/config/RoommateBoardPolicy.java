package org.example.knockin.board.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "policy.board")
public class RoommateBoardPolicy {
    @Min(1)
    private int comeableDateVisibleGraceDays;

    @Min(1)
    private int imageMaxCount;

    @Min(1)
    private int thumbnailImageMaxCount;

    @Min(1)
    private int hotBadgeMinInterestCount;
}
