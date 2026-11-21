package org.example.knockin.mate.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.knockin.global.entity.BaseEntity;
import org.example.knockin.member.entity.Member;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;


@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "roommate_matching_required")
public class RoommateMatchingRequired extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requestee_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member requestee;

    @Column(name = "chatting_room_id", nullable = false)
    private Long chattingRoomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RoommateRequiredStatus status;

    public boolean isRequester(Long requesterId) {
        return this.requester.getId().equals(requesterId);
    }

    public boolean isRequestee(Long requesteeId) {
        return requestee.getId().equals(requesteeId);
    }

    public boolean isPending() {
        return this.status.equals(RoommateRequiredStatus.PENDING);
    }

    public void accept() {
        this.status = RoommateRequiredStatus.ACCEPTED;
    }

    public void reject() {
        this.status = RoommateRequiredStatus.REJECTED;
    }

    public void cancel() {
        this.status = RoommateRequiredStatus.CANCELED;
    }

    public void expire() {
        this.status = RoommateRequiredStatus.EXPIRED;
    }
}
