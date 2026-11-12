package org.example.knockin.life.entity;

import jakarta.persistence.*;
import lombok.*;
@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "preference_condition_weight_log")
public class PreferenceConditionWeightLog {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "member_id")
    private Long memberId;

    @ManyToOne
    @JoinColumn(name = "life_pattern_id")
    private LifePattern lifePattern;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preference_condition_weight_log_degree_id")
    private PreferenceConditionWeightLogDegree preferenceConditionWeightLogDegree;
}
