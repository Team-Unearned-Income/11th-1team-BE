package org.example.knockin.service.impl;

import org.example.knockin.verification.dto.MyVerificationListDto;
import org.example.knockin.meta.service.impl.AlarmServiceImpl;
import org.example.knockin.meta.service.impl.PushNotificationServiceImpl;
import org.example.knockin.verification.dto.BoVerificationApproveListDto;
import org.example.knockin.verification.dto.BoVerificationCancelListDto;
import org.example.knockin.verification.dto.BoVerificationWaitingDetailDto;
import org.example.knockin.verification.dto.BoVerificationWaitingListDto;
import org.example.knockin.verification.dto.EmailConfirmDto;
import org.example.knockin.verification.dto.EmailSendDto;
import org.example.knockin.verification.service.impl.AuthenticationServiceImpl;
import org.example.knockin.verification.entity.ApproveType;
import org.example.knockin.verification.entity.Authentication;
import org.example.knockin.verification.entity.AuthenticationApprove;
import org.example.knockin.verification.entity.AuthenticationType;
import org.example.knockin.member.entity.Member;
import org.example.knockin.global.exception.AuthenticationErrorCode;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.EmailErrorCode;
import org.example.knockin.global.exception.MemberErrorCode;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.example.knockin.verification.repository.AuthenticationApproveRepository;
import org.example.knockin.verification.repository.AuthenticationRepository;
import org.example.knockin.verification.service.MailSendService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("인증 서비스 테스트")
class AuthenticationServiceImplTest {

    @Mock
    private AuthenticationRepository authenticationRepository;

    @Mock
    private AuthenticationApproveRepository authenticationApproveRepository;

    @Mock
    private MemberServiceImpl memberService;

    @Mock
    private MailSendService mailSendService;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private AlarmServiceImpl alarmService;

    @Mock
    private PushNotificationServiceImpl pushNotificationService;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @Test
    @DisplayName("메일 웹훅 처리 성공 테스트")
    void mailWebHookSuccessTest() {
        // given
        String payload = "payload";
        Map<String, String> headers = new HashMap<>();
        headers.put("header-key", "header-value");

        given(mailSendService.mailWebHook(payload, headers)).willReturn(new HashMap<>());

        // when
        String result = authenticationService.mailWebHook(payload, headers);

        // then
        assertThat(result).isEqualTo("webhook success");
        verify(mailSendService).mailWebHook(payload, headers);
    }

    @Test
    @DisplayName("학생 이메일 인증번호 발송 성공 테스트")
    void sendAuthNumStudentSuccessTest() throws IOException {
        // given
        Long memberId = 1L;
        EmailSendDto.Request request = new EmailSendDto.Request("student@univ.ac.kr");

        Member member = mock(Member.class);
        given(memberService.findById(memberId)).willReturn(Optional.of(member));

        Resource resource = mock(Resource.class);
        String mockHtml = "<html>${authToken}</html>";
        given(resourceLoader.getResource("classpath:templates/mail/auth_template.html")).willReturn(resource);
        given(resource.getInputStream()).willReturn(new ByteArrayInputStream(mockHtml.getBytes()));

        given(mailSendService.mailSend(anyString(), anyString(), anyString())).willReturn("email_id_123");

        Authentication oldAuth = spy(Authentication.builder().build());
        given(authenticationRepository.findByMemberIdAndIsDeletedAndIsAcceptedAndType(memberId, false, false, AuthenticationType.STUDENT))
                .willReturn(List.of(oldAuth));

        // when
        EmailSendDto.Response response = authenticationService.sendAuthNumStudent(request, memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(oldAuth).deleteAuthentication();
        verify(authenticationRepository).save(argThat(authentication ->
                memberId.equals(authentication.getMemberId()) && authentication.getType() == AuthenticationType.STUDENT));
    }

    @Test
    @DisplayName("직장인 이메일 인증번호 발송 성공 테스트")
    void sendAuthNumCompanySuccessTest() throws IOException {
        // given
        Long memberId = 1L;
        EmailSendDto.Request request = new EmailSendDto.Request("company@company.com");

        Member member = mock(Member.class);
        given(memberService.findById(memberId)).willReturn(Optional.of(member));

        Resource resource = mock(Resource.class);
        String mockHtml = "<html>${authToken}</html>";
        given(resourceLoader.getResource("classpath:templates/mail/auth_template.html")).willReturn(resource);
        given(resource.getInputStream()).willReturn(new ByteArrayInputStream(mockHtml.getBytes()));

        given(mailSendService.mailSend(anyString(), anyString(), anyString())).willReturn("email_id_123");

        Authentication oldAuth = spy(Authentication.builder().build());
        given(authenticationRepository.findByMemberIdAndIsDeletedAndIsAcceptedAndType(memberId, false, false, AuthenticationType.COMPANY))
                .willReturn(List.of(oldAuth));

        // when
        EmailSendDto.Response response = authenticationService.sendAuthNumCompany(request, memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(oldAuth).deleteAuthentication();
        verify(authenticationRepository).save(argThat(authentication ->
                memberId.equals(authentication.getMemberId()) && authentication.getType() == AuthenticationType.COMPANY));
    }

    @Test
    @DisplayName("이메일 발송 시 회원이 없으면 MemberErrorCode.MEMBER_NOT_FOUND 예외 발생")
    void sendAuthNumMemberNotFoundTest() {
        // given
        Long memberId = 1L;
        EmailSendDto.Request request = new EmailSendDto.Request("test@test.com");
        given(memberService.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authenticationService.sendAuthNumStudent(request, memberId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("학생 이메일 인증번호 검증 성공 테스트")
    void confirmAuthNumStudentSuccessTest() {
        // given
        Long memberId = 1L;
        EmailConfirmDto.Request request = new EmailConfirmDto.Request("student@univ.ac.kr", "123456");

        Member member = mock(Member.class);
        given(memberService.findById(memberId)).willReturn(Optional.of(member));

        Authentication authentication = spy(Authentication.builder()
                .email("student@univ.ac.kr")
                .code("123456")
                .isAccepted(false)
                .isDeleted(false)
                .build());
        ReflectionTestUtils.setField(authentication, "createdAt", LocalDateTime.now());

        given(authenticationRepository.findFirstByMemberIdAndIsDeletedAndIsAcceptedAndTypeOrderByCreatedAtDesc(memberId, false, false, AuthenticationType.STUDENT))
                .willReturn(Optional.of(authentication));

        // when
        EmailConfirmDto.Response response = authenticationService.confirmAuthNumStudent(request, memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(authentication).acceptAuthentication();
    }

    @Test
    @DisplayName("직장인 이메일 인증번호 검증 성공 테스트")
    void confirmAuthNumCompanySuccessTest() {
        // given
        Long memberId = 1L;
        EmailConfirmDto.Request request = new EmailConfirmDto.Request("company@company.com", "123456");

        Member member = mock(Member.class);
        given(memberService.findById(memberId)).willReturn(Optional.of(member));

        Authentication authentication = spy(Authentication.builder()
                .email("company@company.com")
                .code("123456")
                .isAccepted(false)
                .isDeleted(false)
                .build());
        ReflectionTestUtils.setField(authentication, "createdAt", LocalDateTime.now());

        given(authenticationRepository.findFirstByMemberIdAndIsDeletedAndIsAcceptedAndTypeOrderByCreatedAtDesc(memberId, false, false, AuthenticationType.COMPANY))
                .willReturn(Optional.of(authentication));

        // when
        EmailConfirmDto.Response response = authenticationService.confirmAuthNumCompany(request, memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(authentication).acceptAuthentication();
    }

    @Test
    @DisplayName("회원 ID로 승인된 신원 인증 타입 목록을 조회한다")
    void findTypesByMemberIdReturnsAcceptedAuthenticationTypes() {
        // given
        Long memberId = 1L;
        List<AuthenticationType> authenticationTypes = List.of(AuthenticationType.STUDENT, AuthenticationType.COMPANY);
        given(authenticationRepository.getAcceptedAuthenticationTypeByMemberId(memberId)).willReturn(authenticationTypes);

        // when
        List<AuthenticationType> result = authenticationService.findTypesByMemberId(memberId);

        // then
        assertThat(result).containsExactly(AuthenticationType.STUDENT, AuthenticationType.COMPANY);
        verify(authenticationRepository).getAcceptedAuthenticationTypeByMemberId(memberId);
    }

    @Test
    @DisplayName("인증번호 유효시간(3분) 경과 시 예외 발생 테스트")
    void confirmAuthNumTimeoutTest() {
        // given
        Long memberId = 1L;
        EmailConfirmDto.Request request = new EmailConfirmDto.Request("student@univ.ac.kr", "123456");

        Member member = mock(Member.class);
        given(memberService.findById(memberId)).willReturn(Optional.of(member));

        Authentication authentication = Authentication.builder()
                .email("student@univ.ac.kr")
                .code("123456")
                .build();
        // 4분 전 생성된 것으로 설정
        ReflectionTestUtils.setField(authentication, "createdAt", LocalDateTime.now().minusMinutes(4));

        given(authenticationRepository.findFirstByMemberIdAndIsDeletedAndIsAcceptedAndTypeOrderByCreatedAtDesc(memberId, false, false, AuthenticationType.STUDENT))
                .willReturn(Optional.of(authentication));

        // when & then
        assertThatThrownBy(() -> authenticationService.confirmAuthNumStudent(request, memberId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", EmailErrorCode.EMAIL_VALID_ERROR);
    }

    @Test
    @DisplayName("인증번호가 다르면 예외 발생 테스트")
    void confirmAuthNumMismatchTest() {
        // given
        Long memberId = 1L;
        EmailConfirmDto.Request request = new EmailConfirmDto.Request("student@univ.ac.kr", "wrong_code");

        Member member = mock(Member.class);
        given(memberService.findById(memberId)).willReturn(Optional.of(member));

        Authentication authentication = Authentication.builder()
                .email("student@univ.ac.kr")
                .code("123456")
                .build();
        ReflectionTestUtils.setField(authentication, "createdAt", LocalDateTime.now());

        given(authenticationRepository.findFirstByMemberIdAndIsDeletedAndIsAcceptedAndTypeOrderByCreatedAtDesc(memberId, false, false, AuthenticationType.STUDENT))
                .willReturn(Optional.of(authentication));

        // when & then
        assertThatThrownBy(() -> authenticationService.confirmAuthNumStudent(request, memberId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", EmailErrorCode.EMAIL_VALID_ERROR);
    }

    @Test
    @DisplayName("승인 완료된 인증 목록 페이징 조회 성공 테스트")
    void findVerificationApprovesTest() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        BoVerificationApproveListDto.Response.EmployeeAuthItem item = new BoVerificationApproveListDto.Response.EmployeeAuthItem();
        given(authenticationRepository.findVerificationApproves(pageable)).willReturn(List.of(item));

        // when
        List<BoVerificationApproveListDto.Response.EmployeeAuthItem> result = authenticationService.findVerificationApproves(pageable);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("반려/취소된 인증 목록 페이징 조회 성공 테스트")
    void findVerificationCancelsTest() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        BoVerificationCancelListDto.Response.EmployeeAuthItem item = new BoVerificationCancelListDto.Response.EmployeeAuthItem();
        given(authenticationRepository.findVerificationCancels(pageable)).willReturn(List.of(item));

        // when
        List<BoVerificationCancelListDto.Response.EmployeeAuthItem> result = authenticationService.findVerificationCancels(pageable);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("인증 대기 목록 페이징 조회 성공 테스트")
    void findVerificationsListTest() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        BoVerificationWaitingListDto.Response.EmployeeAuthItem item = new BoVerificationWaitingListDto.Response.EmployeeAuthItem();
        given(authenticationRepository.findVerificationsList(pageable)).willReturn(List.of(item));

        // when
        List<BoVerificationWaitingListDto.Response.EmployeeAuthItem> result = authenticationService.findVerificationsList(pageable);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("인증 대기 상세 조회 및 경과일 계산 성공 테스트")
    void findVerificationsDetailTest() {
        // given
        Long id = 1L;
        BoVerificationWaitingDetailDto.Response detail = new BoVerificationWaitingDetailDto.Response();
        detail.setCreateAt(LocalDateTime.now().minusDays(5));

        given(authenticationRepository.findVerifications(id)).willReturn(detail);

        // when
        BoVerificationWaitingDetailDto.Response result = authenticationService.findVerifications(id);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getElapsedAt()).isEqualTo(5);
    }

    @Test
    @DisplayName("인증 승인 등록 성공 테스트")
    void saveVerificationsSuccessTest() {
        // given
        Long id = 1L;
        Member member = Member.builder().id(10L).fcmToken("token").build();
        Authentication authentication = Authentication.builder().memberId(member.getId()).build();
        AuthenticationApprove approve = AuthenticationApprove.builder()
                .id(id)
                .authentication(authentication)
                .status(ApproveType.PENDING)
                .build();
        given(authenticationApproveRepository.findById(id)).willReturn(Optional.of(approve));
        given(memberService.findById(member.getId())).willReturn(Optional.of(member));

        // when
        authenticationService.saveVerifications(id);

        // then
        assertThat(approve.getStatus()).isEqualTo(ApproveType.ACCEPTED);
        assertThat(approve.getRejectReason()).isNull();
    }

    @Test
    @DisplayName("인증 승인 등록 시 대상 인증건 미존재 시 예외 발생 테스트")
    void saveVerificationsNotFoundTest() {
        // given
        Long id = 1L;
        given(authenticationApproveRepository.findById(id)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authenticationService.saveVerifications(id))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthenticationErrorCode.AUTHENTICATION_NOT_FOUNT);
    }

    @Test
    @DisplayName("인증에 연결된 회원이 없으면 승인 알림을 발송하지 않는다")
    void saveVerificationsRejectsMissingMember() {
        Long id = 1L;
        Long memberId = 10L;
        Authentication authentication = Authentication.builder().memberId(memberId).build();
        AuthenticationApprove approve = AuthenticationApprove.builder()
                .id(id)
                .authentication(authentication)
                .status(ApproveType.PENDING)
                .build();
        given(authenticationApproveRepository.findById(id)).willReturn(Optional.of(approve));
        given(memberService.findById(memberId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.saveVerifications(id))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.MEMBER_NOT_FOUND);
        verifyNoInteractions(alarmService, pushNotificationService);
    }

    @Test
    @DisplayName("인증 반려 등록 성공 테스트")
    void deleteVerificationsSuccessTest() {
        // given
        Long id = 1L;
        String rejectReason = "반려사유";
        Member member = Member.builder().id(10L).fcmToken("token").build();
        Authentication authentication = Authentication.builder().memberId(member.getId()).build();
        AuthenticationApprove approve = AuthenticationApprove.builder()
                .id(id)
                .authentication(authentication)
                .status(ApproveType.PENDING)
                .build();
        given(authenticationApproveRepository.findById(id)).willReturn(Optional.of(approve));
        given(memberService.findById(member.getId())).willReturn(Optional.of(member));

        // when
        authenticationService.deleteVerifications(id, rejectReason);

        // then
        assertThat(approve.getStatus()).isEqualTo(ApproveType.REJECT);
        assertThat(approve.getRejectReason()).isEqualTo(rejectReason);
    }

    @Test
    @DisplayName("내 인증 현황 목록 조회 성공 테스트")
    void findVerificationListSuccessTest() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Long memberId = 1L;
        MyVerificationListDto.Response.AuthInfo studentAuth = MyVerificationListDto.Response.AuthInfo.builder()
                .status(ApproveType.ACCEPTED)
                .email("student@univ.ac.kr")
                .createAt(LocalDateTime.now())
                .build();
        MyVerificationListDto.Response.AuthInfo employeeAuth = MyVerificationListDto.Response.AuthInfo.builder()
                .status(ApproveType.ACCEPTED)
                .email("employee@company.com")
                .createAt(LocalDateTime.now())
                .build();

        given(authenticationRepository.findVerificationList(pageable, memberId, AuthenticationType.STUDENT)).willReturn(Optional.of(studentAuth));
        given(authenticationRepository.findVerificationList(pageable, memberId, AuthenticationType.COMPANY)).willReturn(Optional.of(employeeAuth));

        // when
        MyVerificationListDto.Response response = authenticationService.findVerificationList(pageable, memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStudentAuth()).isNotNull();
        assertThat(response.getStudentAuth().getStatus()).isEqualTo(ApproveType.ACCEPTED);
        assertThat(response.getStudentAuth().getEmail()).isEqualTo("student@univ.ac.kr");
        assertThat(response.getEmployeeAuth()).isNotNull();
        assertThat(response.getEmployeeAuth().getStatus()).isEqualTo(ApproveType.ACCEPTED);
        assertThat(response.getEmployeeAuth().getEmail()).isEqualTo("employee@company.com");
    }
}
