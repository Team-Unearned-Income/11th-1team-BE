package org.example.knockin.verification.repository;

import org.example.knockin.verification.entity.Authentication;
import org.example.knockin.verification.entity.AuthenticationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthenticationRepository extends JpaRepository<Authentication, Long>, AuthenticationRepositoryCustom {
    List<Authentication> findByMemberIdAndIsDeletedAndIsAccepted(Long memberId, Boolean isDeleted, Boolean isAccepted);

    List<Authentication> findByMemberIdAndIsDeletedAndIsAcceptedAndType(Long memberId, Boolean isDeleted, Boolean isAccepted, AuthenticationType type);
    Optional<Authentication> findFirstByMemberIdAndIsDeletedAndIsAcceptedAndTypeOrderByCreatedAtDesc(Long memberId, Boolean isDeleted, Boolean isAccepted, AuthenticationType type);
}