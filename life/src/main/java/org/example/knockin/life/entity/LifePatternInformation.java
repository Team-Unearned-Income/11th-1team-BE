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
import lombok.*;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "life_pattern_information")
public class LifePatternInformation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "life_pattern_id", nullable = false)
    private LifePattern lifePattern;

    @Column(name = "dvalue", nullable = false, length = 50)
    private String dvalue;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    public void modifyLifePatternInformation(String dvalue, String description) {
        this.dvalue = dvalue;
        this.description = description;
    }
}
