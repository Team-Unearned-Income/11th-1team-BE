package org.example.knockin.verification.repository;

import java.util.List;

import org.example.knockin.verification.dto.BoVerificationApproveListDto;
import org.example.knockin.verification.dto.BoVerificationCancelListDto;
import org.example.knockin.verification.dto.BoVerificationWaitingDetailDto;
import org.example.knockin.verification.dto.BoVerificationWaitingListDto;
import org.example.knockin.verification.dto.MyVerificationListDto;
import org.example.knockin.verification.entity.AuthenticationType;
import org.example.knockin.verification.repository.row.MemberAuthenticationRow;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface AuthenticationRepositoryCustom {
    List<AuthenticationType> getAcceptedAuthenticationTypeByMemberId(Long memberId);

    List<MemberAuthenticationRow> findAcceptedByMemberIds(List<Long> memberIds);

    List<BoVerificationApproveListDto.Response.EmployeeAuthItem> findVerificationApproves(Pageable pageable);

    List<BoVerificationCancelListDto.Response.EmployeeAuthItem> findVerificationCancels(Pageable pageable);

    List<BoVerificationWaitingListDto.Response.EmployeeAuthItem> findVerificationsList(Pageable pageable);

    BoVerificationWaitingDetailDto.Response findVerifications(Long id);

    Optional<MyVerificationListDto.Response.AuthInfo> findVerificationList(Pageable pageable, Long memberId, AuthenticationType authenticationType);
}
