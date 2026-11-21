package org.example.knockin.mate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter
@Embeddable
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoommateCalendarMemberId implements Serializable {
    @Column(name = "roommate_calendar_id")
    Long roommateCalendarId;
    @Column(name = "member_id")
    Long memberId;
}
