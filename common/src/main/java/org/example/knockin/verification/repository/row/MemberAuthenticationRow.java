package org.example.knockin.verification.repository.row;

import org.example.knockin.verification.entity.AuthenticationType;

public record MemberAuthenticationRow(
        Long memberId,
        AuthenticationType type
) {
}
