package org.example.knockin.inquiry.service;

public record InquiryReplyEvent(
        Long memberId,
        Long inquiryId
) {
}
