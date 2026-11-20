package org.example.knockin.verification.event;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.knockin.verification.entity.AuthenticationType;

@Getter
@RequiredArgsConstructor
public class MemberAuthenticationTypesQueryEvent {
    private final Long memberId;
    private List<AuthenticationType> result;

    public void complete(List<AuthenticationType> result) {
        this.result = List.copyOf(result);
    }

    public List<AuthenticationType> getResult() {
        if (result == null) {
            throw new IllegalStateException("MemberAuthenticationTypesQueryEvent was not handled synchronously");
        }
        return result;
    }
}
