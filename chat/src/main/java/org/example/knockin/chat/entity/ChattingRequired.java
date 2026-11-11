package org.example.knockin.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.knockin.global.entity.CreatedAtEntity;


@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chatting_required")
public class ChattingRequired extends CreatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "requester", nullable = false)
    private Long requesterId;

    @Column(name = "requestee", nullable = false)
    private Long requesteeId;

    @Column(name = "roommate_board_id")
    private Long roommateBoardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ChattingRequiredStatus status;

    public boolean isPending() {
        return this.status == ChattingRequiredStatus.PENDING;
    }

    public void accept() {
        this.status = ChattingRequiredStatus.ACCEPTED;
    }

    public void reject() {
        this.status = ChattingRequiredStatus.REJECTED;
    }

    public void cancel() {
        this.status = ChattingRequiredStatus.CANCELED;
    }
}
