package org.example.knockin.support;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.resend.Resend;
import com.svix.Webhook;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.example.knockin.global.auth.util.AppleClientSecretGenerator;
import org.example.knockin.alarm.config.FirebaseConfig;
import org.example.knockin.global.config.ResendConfig;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Full-context tests use local persistence and mocks for external SDKs and credentials. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@TestPropertySource(properties = {
        "kakao.admin.key=test-only-kakao-key",
        "jwt.key=test-only-signing-key-01234567890123456789012345678901234567890123456789",
        "batch.member-delete.cron=-",
        "spring.batch.job.enabled=false"
})
@MockitoBean(types = {
        FirebaseConfig.class, FirebaseApp.class, FirebaseMessaging.class,
        ResendConfig.class, Resend.class, Webhook.class,
        AppleClientSecretGenerator.class, ClientRegistrationRepository.class
})
public @interface IsolatedExternalServices {
}
