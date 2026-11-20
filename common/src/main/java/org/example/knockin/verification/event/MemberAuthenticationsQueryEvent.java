package org.example.knockin.verification.event;

import java.util.List;
import lombok.Getter;
import org.example.knockin.verification.repository.row.MemberAuthenticationRow;

@Getter
public class MemberAuthenticationsQueryEvent {
    private final List<Long> memberIds;
    private List<MemberAuthenticationRow> result;

    public MemberAuthenticationsQueryEvent(List<Long> memberIds) {
        this.memberIds = List.copyOf(memberIds);
    }

    public void complete(List<MemberAuthenticationRow> result) {
        this.result = List.copyOf(result);
    }

    public List<MemberAuthenticationRow> getResult() {
        if (result == null) {
            throw new IllegalStateException("MemberAuthenticationsQueryEvent was not handled synchronously");
        }
        return result;
    }
}
