package org.example.knockin.life.entity;

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
import org.example.knockin.global.entity.BaseEntity;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "member_life_pattern",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_life_pattern",
                columnNames = {"member_id", "life_pattern_information_id"}
        )
)
public class MemberLifePattern extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "life_pattern_information_id", nullable = false)
    private LifePatternInformation lifePatternInformation;

    public void modifyLifePatternInformation(LifePatternInformation lifePatternInformation) {
        this.lifePatternInformation = lifePatternInformation;
    }
}
