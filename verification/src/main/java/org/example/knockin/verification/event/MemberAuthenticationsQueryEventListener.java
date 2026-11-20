package org.example.knockin.verification.event;

import lombok.RequiredArgsConstructor;
import org.example.knockin.verification.repository.AuthenticationRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberAuthenticationsQueryEventListener {
    private final AuthenticationRepository authenticationRepository;

    @EventListener
    public void handle(MemberAuthenticationsQueryEvent event) {
        event.complete(authenticationRepository.findAcceptedByMemberIds(event.getMemberIds()));
    }

    @EventListener
    public void handle(MemberAuthenticationTypesQueryEvent event) {
        event.complete(authenticationRepository.getAcceptedAuthenticationTypeByMemberId(event.getMemberId()));
    }
}
