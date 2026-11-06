package org.example.knockin.verification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.*;
import org.example.knockin.global.entity.CreatedAtEntity;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "authentication_approve")
public class AuthenticationApprove extends CreatedAtEntity {
    @Id
    @Column(name = "authentication_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authentication_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Authentication authentication;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApproveType status;

    @Column(length = 500)
    private String rejectReason;

    public void approve() {
        this.status = ApproveType.ACCEPTED;
        this.rejectReason = null;
    }

    public void reject(String rejectReason) {
        this.status = ApproveType.REJECT;
        this.rejectReason = rejectReason;
    }
}
