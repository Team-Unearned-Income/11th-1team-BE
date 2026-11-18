package org.example.knockin.integration;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.knockin.alarm.port.FcmTokenReader;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.MemberErrorCode;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.repository.MemberRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberFcmTokenAdapter implements FcmTokenReader {

    private final MemberRepository memberRepository;

    @Override
    public Optional<String> readFcmTokenById(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        return Optional.ofNullable(member.getFcmToken());
    }
}
