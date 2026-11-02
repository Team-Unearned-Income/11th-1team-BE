package org.example.knockin.agreement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.knockin.global.entity.CreatedAtEntity;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "agreement")
public class Agreement extends CreatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_type_id")
    private AgreementType type;

    @Column(name = "title", length = 50)
    private String title;

    @Column(name = "contents", columnDefinition = "TEXT")
    private String contents;

    @Builder.Default
    @ColumnDefault("false")
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "is_required")
    private Boolean isRequired;

    public void modifyAgreement(Agreement agreement) {
        this.title = agreement.getTitle();
        this.contents = agreement.getContents();
        this.isRequired = agreement.getIsRequired();
    }

    public void deleteAgreement() {
        this.isDeleted = true;
    }
}
