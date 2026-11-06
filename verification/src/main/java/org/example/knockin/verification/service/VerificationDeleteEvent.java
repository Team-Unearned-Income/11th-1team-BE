package org.example.knockin.verification.service;

public record VerificationDeleteEvent(
        Long memberId,
        String rejectReason
) {
}
