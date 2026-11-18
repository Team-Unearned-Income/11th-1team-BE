package org.example.knockin.member.repository;

import org.example.knockin.member.entity.BasicInformation;
import org.example.knockin.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BasicInformationRepository extends JpaRepository<BasicInformation, Long>, BasicInformationRepositoryCustom {
    List<BasicInformation> findByMember(Member member);

    boolean existsByMemberId(Long memberId);
}