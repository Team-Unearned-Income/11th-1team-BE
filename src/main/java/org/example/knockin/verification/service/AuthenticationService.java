package org.example.knockin.verification.service;

import java.util.List;
import java.util.Map;
import org.example.knockin.verification.dto.BoVerificationApproveListDto;
import org.example.knockin.verification.dto.BoVerificationApproveListDto.Response.EmployeeAuthItem;
import org.example.knockin.verification.dto.BoVerificationCancelListDto;
import org.example.knockin.verification.dto.BoVerificationWaitingDetailDto;
import org.example.knockin.verification.dto.BoVerificationWaitingListDto;
import org.example.knockin.verification.dto.EmailConfirmDto;
import org.example.knockin.verification.dto.EmailSendDto;
import org.example.knockin.verification.dto.MyVerificationListDto;
import org.example.knockin.verification.entity.AuthenticationType;
import org.example.knockin.verification.repository.row.MemberAuthenticationRow;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface AuthenticationService {
    String mailWebHook(String payload, Map<String, String> headers);

    @Transactional
    EmailSendDto.Response sendAuthNumStudent(EmailSendDto.Request request, Long memberId);

    @Transactional(rollbackFor = Exception.class)
    EmailSendDto.Response sendAuthNumCompany(EmailSendDto.Request request, Long memberId);

    @Transactional
    EmailConfirmDto.Response confirmAuthNumStudent(EmailConfirmDto.Request request, Long memberId);

    @Transactional
    EmailConfirmDto.Response confirmAuthNumCompany(EmailConfirmDto.Request request, Long memberId);

    String generateAuthCode();

    String loadMailTemplate(String authToken);

    List<EmployeeAuthItem> findVerificationApproves(Pageable pageable);

    List<BoVerificationCancelListDto.Response.EmployeeAuthItem> findVerificationCancels(Pageable pageable);

    List<BoVerificationWaitingListDto.Response.EmployeeAuthItem> findVerificationsList(Pageable pageable);

    BoVerificationWaitingDetailDto.Response findVerifications(Long id);

    @Transactional
    void saveVerifications(Long id);

    @Transactional
    void deleteVerifications(Long id, String rejectReason);

    List<AuthenticationType> findTypesByMemberId(Long memberId);

    List<MemberAuthenticationRow> findAcceptedByMemberIds(List<Long> memberIds);

    MyVerificationListDto.Response findVerificationList(Pageable pageable, Long memberId);
}
