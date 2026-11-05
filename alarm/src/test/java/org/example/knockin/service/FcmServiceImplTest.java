package org.example.knockin.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.example.knockin.alarm.service.impl.FcmServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FCM 서비스 테스트")
class FcmServiceImplTest {

    @Mock
    private FirebaseMessaging firebaseMessaging;


    @InjectMocks
    private FcmServiceImpl fcmServiceImpl;

    @Test
    @DisplayName("FCM 전송이 실패해도 호출자에게 예외를 전파하지 않는다")
    void sendDoesNotPropagateFirebaseFailure() throws Exception {
        // given
        FirebaseMessagingException firebaseException = mock(FirebaseMessagingException.class);
        given(firebaseMessaging.send(any(Message.class))).willThrow(firebaseException);

        // when & then
        assertThatCode(() -> fcmServiceImpl.send(
                "제목",
                "본문",
                "fcm-token",
                "knockinrn://chat/10"
        )).doesNotThrowAnyException();
        verify(firebaseMessaging).send(any(Message.class));
    }
}
