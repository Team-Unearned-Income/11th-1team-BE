package org.example.knockin.mate.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.knockin.global.entity.CreatedAtEntity;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "roommate_calendar_category")
public class RoommateCalendarCategory extends CreatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Builder.Default
    @ColumnDefault("false")
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    public void rename(String name) {
        this.name = name;
    }
}
