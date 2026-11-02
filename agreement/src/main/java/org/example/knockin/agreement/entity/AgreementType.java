package org.example.knockin.agreement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.knockin.global.entity.CreatedAtEntity;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "agreement_type")
public class AgreementType extends CreatedAtEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50)
    private String name;
    private Boolean isDeleted;

    public void modifyAgreementType(String name) {
        this.name = name;
    }

    public void deleteAgreementType() {
        this.isDeleted = true;
    }
}
