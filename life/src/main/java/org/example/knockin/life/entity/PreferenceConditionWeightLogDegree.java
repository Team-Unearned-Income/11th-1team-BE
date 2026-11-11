package org.example.knockin.life.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "preference_condition_weight_log_degree")
public class PreferenceConditionWeightLogDegree {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long degree;
}
