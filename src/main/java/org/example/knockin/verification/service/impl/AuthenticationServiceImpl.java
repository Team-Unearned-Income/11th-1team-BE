package org.example.knockin.verification.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.knockin.verification.dto.MyVerificationListDto;
import org.example.knockin.meta.service.impl.AlarmServiceImpl;
import org.example.knockin.meta.service.impl.PushNotificationServiceImpl;
import org.example.knockin.verification.dto.BoVerificationApproveListDto;
import org.example.knockin.verification.dto.BoVerificationCancelListDto;
import org.example.knockin.verification.dto.BoVerificationWaitingDetailDto;
import org.example.knockin.verification.dto.BoVerificationWaitingListDto;
import org.example.knockin.verification.dto.EmailConfirmDto;
import org.example.knockin.verification.dto.EmailSendDto;
import org.example.knockin.verification.entity.ApproveType;
import org.example.knockin.global.entity.AuthenticateAlarmTemplate;
import org.example.knockin.verification.entity.Authentication;
import org.example.knockin.verification.entity.AuthenticationApprove;
import org.example.knockin.verification.entity.AuthenticationType;
import org.example.knockin.meta.entity.Alarm;
import org.example.knockin.meta.entity.AlarmSettingType;
import org.example.knockin.meta.entity.AlarmType;
import org.example.knockin.member.entity.Member;
import org.example.knockin.global.exception.AuthenticationErrorCode;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.EmailErrorCode;
import org.example.knockin.global.exception.MemberErrorCode;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.example.knockin.verification.repository.AuthenticationApproveRepository;
import org.example.knockin.verification.repository.AuthenticationRepository;
import org.example.knockin.verification.repository.row.MemberAuthenticationRow;
import org.example.knockin.verification.service.MailSendService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl {
    private final AuthenticationRepository authenticationRepository;
    private final AuthenticationApproveRepository authenticationApproveRepository;
    private final MemberServiceImpl memberService;
    private final MailSendService mailSendService;
    private final ResourceLoader resourceLoader;
    private final AlarmServiceImpl alarmService;
    private final PushNotificationServiceImpl pushNotificationService;

    private final String mailSubject = "[노크인] 신원 인증을 완료해 주세요.";
    private final int validTime = 3;

    public String mailWebHook(String payload, Map<String, String> headers) {
        mailSendService.mailWebHook(payload, headers);
        return "webhook success";
    }

    @Transactional
    public EmailSendDto.Response sendAuthNumStudent(EmailSendDto.Request request, Long memberId) {
        memberService.findById(memberId).orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
        String code = generateAuthCode();
        String mailContent = loadMailTemplate(code);
        Object result = mailSendService.mailSend(request.getEmail(), mailSubject, mailContent);
        if(ObjectUtils.isEmpty(result)) throw new BusinessException(EmailErrorCode.EMAIL_SEND_ERROR);
        authenticationRepository.findByMemberIdAndIsDeletedAndIsAcceptedAndType(memberId, false, false, AuthenticationType.STUDENT).forEach(Authentication::deleteAuthentication);
        authenticationRepository.save(Authentication.builder().memberId(memberId).type(AuthenticationType.STUDENT).email(request.getEmail()).code(code).isAccepted(false).isDeleted(false).build());
        return EmailSendDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional(rollbackFor = Exception.class)
    public EmailSendDto.Response sendAuthNumCompany(EmailSendDto.Request request, Long memberId) {
        memberService.findById(memberId).orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
        String code = generateAuthCode();
        String mailContent = loadMailTemplate(code);
        Object result = mailSendService.mailSend(request.getEmail(), mailSubject, mailContent);
        if(ObjectUtils.isEmpty(result)) throw new BusinessException(EmailErrorCode.EMAIL_SEND_ERROR);
        authenticationRepository.findByMemberIdAndIsDeletedAndIsAcceptedAndType(memberId, false, false, AuthenticationType.COMPANY).forEach(Authentication::deleteAuthentication);
        authenticationRepository.save(Authentication.builder().memberId(memberId).type(AuthenticationType.COMPANY).email(request.getEmail()).code(code).isAccepted(false).isDeleted(false).build());
        return EmailSendDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public EmailConfirmDto.Response confirmAuthNumStudent(EmailConfirmDto.Request request, Long memberId) {
        memberService.findById(memberId).orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
        Authentication authentication = authenticationRepository.findFirstByMemberIdAndIsDeletedAndIsAcceptedAndTypeOrderByCreatedAtDesc(memberId, false, false, AuthenticationType.STUDENT).orElseThrow(() -> new BusinessException(EmailErrorCode.EMAIL_VALID_ERROR));

        if (authentication.getCreatedAt().plusMinutes(validTime).isBefore(LocalDateTime.now())) {
            throw new BusinessException(EmailErrorCode.EMAIL_VALID_ERROR);
        }

        if(authentication.getCode().equals(request.getAuthNo()) && authentication.getEmail().equals(request.getEmail())) {
            authentication.acceptAuthentication();
            authenticationApproveRepository.save(AuthenticationApprove.builder().authentication(authentication).status(
                    ApproveType.PENDING).build());
        } else {
            throw new BusinessException(EmailErrorCode.EMAIL_VALID_ERROR);
        }

        return EmailConfirmDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public EmailConfirmDto.Response confirmAuthNumCompany(EmailConfirmDto.Request request, Long memberId) {
        memberService.findById(memberId).orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
        Authentication authentication = authenticationRepository.findFirstByMemberIdAndIsDeletedAndIsAcceptedAndTypeOrderByCreatedAtDesc(memberId, false, false, AuthenticationType.COMPANY).orElseThrow(() -> new BusinessException(EmailErrorCode.EMAIL_VALID_ERROR));

        if (authentication.getCreatedAt().plusMinutes(validTime).isBefore(LocalDateTime.now())) {
            throw new BusinessException(EmailErrorCode.EMAIL_VALID_ERROR);
        }

        if(authentication.getCode().equals(request.getAuthNo()) && authentication.getEmail().equals(request.getEmail())) {
            authentication.acceptAuthentication();
            authenticationApproveRepository.save(AuthenticationApprove.builder().authentication(authentication).status(ApproveType.PENDING).build());
        } else {
            throw new BusinessException(EmailErrorCode.EMAIL_VALID_ERROR);
        }

        return EmailConfirmDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    private String generateAuthCode() {
        return String.format("%06d", java.util.concurrent.ThreadLocalRandom.current().nextInt(1000000));
    }

    private String loadMailTemplate(String authToken) {
        try {
            Resource resource = resourceLoader.getResource("classpath:templates/mail/auth_template.html");
            String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return template.replace("${authToken}", authToken);
        } catch (IOException e) {
            throw new BusinessException(EmailErrorCode.EMAIL_TEMPLATE_LOAD_ERROR);
        }
    }

    public List<BoVerificationApproveListDto.Response.EmployeeAuthItem> findVerificationApproves(Pageable pageable) {
        return authenticationRepository.findVerificationApproves(pageable);
    }

    public List<BoVerificationCancelListDto.Response.EmployeeAuthItem> findVerificationCancels(Pageable pageable) {
        return authenticationRepository.findVerificationCancels(pageable);
    }

    public List<BoVerificationWaitingListDto.Response.EmployeeAuthItem> findVerificationsList(Pageable pageable) {
        return authenticationRepository.findVerificationsList(pageable);
    }

    public BoVerificationWaitingDetailDto.Response findVerifications(Long id) {
        BoVerificationWaitingDetailDto.Response response = authenticationRepository.findVerifications(id);
        LocalDateTime now = LocalDateTime.now();
        long days = Duration.between(response.getCreateAt(), now).toDays();
        response.setElapsedAt((int) days);
        return response;
    }

    @Transactional
    public void saveVerifications(Long id) {
        AuthenticationApprove approve = authenticationApproveRepository.findById(id).orElseThrow(() -> new BusinessException(AuthenticationErrorCode.AUTHENTICATION_NOT_FOUNT));
        approve.approve();

        Member member = memberService.findById(approve.getAuthentication().getMemberId())
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
        Alarm alarm = Alarm.builder()
                .title(AuthenticateAlarmTemplate.SAVE_VERIFICATION.formatTitle())
                .contents(AuthenticateAlarmTemplate.SAVE_VERIFICATION.formatContents())
                .isRead(false)
                .member(member)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .type(AlarmType.DEFAULT)
                .build();

        alarmService.sendToClient(member.getId(), AuthenticateAlarmTemplate.SAVE_VERIFICATION.name(), alarm);
        pushNotificationService.send(member, AlarmSettingType.NOTIFICATION, AuthenticateAlarmTemplate.SAVE_VERIFICATION.formatTitle(), AuthenticateAlarmTemplate.SAVE_VERIFICATION.formatContents(), AuthenticateAlarmTemplate.SAVE_VERIFICATION.formatDeepLink());
    }

    @Transactional
    public void deleteVerifications(Long id, String rejectReason) {
        AuthenticationApprove approve = authenticationApproveRepository.findById(id).orElseThrow(() -> new BusinessException(AuthenticationErrorCode.AUTHENTICATION_NOT_FOUNT));
        approve.reject(rejectReason);

        Member member = memberService.findById(approve.getAuthentication().getMemberId())
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
        Alarm alarm = Alarm.builder()
                .title(AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatTitle())
                .contents(AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatContents(rejectReason))
                .isRead(false)
                .member(member)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .type(AlarmType.DEFAULT)
                .build();

        alarmService.sendToClient(member.getId(), AuthenticateAlarmTemplate.DELETE_VERIFICATION.name(), alarm);
        pushNotificationService.send(member, AlarmSettingType.NOTIFICATION, AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatTitle(), AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatContents(rejectReason), AuthenticateAlarmTemplate.DELETE_VERIFICATION.formatDeepLink());
    }

    public List<AuthenticationType> findTypesByMemberId(Long memberId) {
        return authenticationRepository.getAcceptedAuthenticationTypeByMemberId(memberId);
    }

    public List<MemberAuthenticationRow> findAcceptedByMemberIds(List<Long> memberIds) {
        return authenticationRepository.findAcceptedByMemberIds(memberIds);
    }

    public MyVerificationListDto.Response findVerificationList(Pageable pageable, Long memberId) {
        MyVerificationListDto.Response.AuthInfo studentAuth = authenticationRepository.findVerificationList(pageable, memberId, AuthenticationType.STUDENT).orElse(null);
        MyVerificationListDto.Response.AuthInfo employeeAuth = authenticationRepository.findVerificationList(pageable, memberId, AuthenticationType.COMPANY).orElse(null);

        return MyVerificationListDto.Response.builder()
                .studentAuth(studentAuth)
                .employeeAuth(employeeAuth)
                .build();
    }
}
