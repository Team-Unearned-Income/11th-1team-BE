package org.example.knockin.agreement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;
import org.example.knockin.global.entity.CreatedAtEntity;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "member_agreement",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_agreement_log",
                columnNames = {"member_id", "agreement_log_id"}
        )
)
public class MemberAgreement extends CreatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_log_id", nullable = false)
    private AgreementLog agreementLog;

    @Column(name = "is_agreed", nullable = false)
    private Boolean isAgreed;

    public void disableAgree() {
        this.isAgreed = false;
    }
}
