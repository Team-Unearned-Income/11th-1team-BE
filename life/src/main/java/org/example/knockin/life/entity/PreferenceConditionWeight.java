package org.example.knockin.life.entity;

import jakarta.persistence.*;
import lombok.*;
@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "preference_condition_weight")
public class PreferenceConditionWeight {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "member_id")
    private Long memberId;

    @ManyToOne
    @JoinColumn(name = "life_pattern_id")
    private LifePattern lifePattern;

    public void modifyLifePattern(LifePattern lifePattern) {
        this.lifePattern = lifePattern;
    }
}
