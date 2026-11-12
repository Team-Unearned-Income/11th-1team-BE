package org.example.knockin.life.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.example.knockin.global.entity.CreatedAtEntity;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "life_pattern")
public class LifePattern extends CreatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "life_pattern_description", nullable = false, length = 100)
    private String lifePatternDescription;

    @Column(name = "preference_description", nullable = false, length = 100)
    private String preferenceDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "dtype", nullable = false, length = 30)
    private LifePatternType dtype;

    @Builder.Default
    @ColumnDefault("false")
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "sort", nullable = false)
    private Integer sort;

    public void modifyLifePattern(String name, LifePatternType type, Integer sort, String lifePatternDescription, String preferenceDescription) {
        this.name = name;
        this.dtype = type;
        this.sort = sort;
        this.isDeleted = false;
        this.lifePatternDescription = lifePatternDescription;
        this.preferenceDescription = preferenceDescription;
    }

    public void deleteLifePattern() {
        this.isDeleted = true;
    }
}
