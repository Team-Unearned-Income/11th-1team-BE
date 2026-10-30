package org.example.knockin.declaration.repository;

import org.example.knockin.declaration.entity.MemberDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberDeclarationRepository extends JpaRepository<MemberDeclaration, Long>,
        MemberDeclarationRepositoryCustom {
    boolean existsByReporterIdAndReportedId(Long reporterId, Long reportedId);
}
