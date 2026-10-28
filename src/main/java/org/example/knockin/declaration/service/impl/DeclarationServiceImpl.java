package org.example.knockin.declaration.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.knockin.board.entity.RoommateBoard;
import org.example.knockin.declaration.entity.RoommateBoardDeclaration;
import org.example.knockin.declaration.dto.BoReportDoneListDto;
import org.example.knockin.declaration.dto.BoReportWaitListDto;
import org.example.knockin.global.exception.RoommateBoardErrorCode;
import org.example.knockin.member.entity.Member;
import org.example.knockin.declaration.entity.MemberDeclaration;
import org.example.knockin.global.exception.MemberErrorCode;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.DeclarationErrorCode;
import org.example.knockin.declaration.entity.DeclarationType;
import org.example.knockin.global.util.ReportType;
import org.example.knockin.declaration.repository.RoommateBoardDeclarationRepository;
import org.example.knockin.declaration.repository.MemberDeclarationRepository;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class DeclarationServiceImpl {
    private final MemberDeclarationRepository memberDeclarationRepository;
    private final RoommateBoardDeclarationRepository roommateBoardDeclarationRepository;
    private final MemberServiceImpl memberService;

    public List<BoReportWaitListDto.Response.ReportInfo> findReportWaitList(Pageable pageable) {
        List<BoReportWaitListDto.Response.ReportInfo> memberReports = memberDeclarationRepository.findReportWaitList(pageable);
        List<BoReportWaitListDto.Response.ReportInfo> boardReports = roommateBoardDeclarationRepository.findReportWaitList(pageable);

        return Stream.concat(memberReports.stream(), boardReports.stream()).sorted(Comparator.comparing(BoReportWaitListDto.Response.ReportInfo::getCreatedAt).reversed()).toList();
    }

    public List<BoReportDoneListDto.Response.ReportInfo> findReportDoneList(Pageable pageable) {
        List<BoReportDoneListDto.Response.ReportInfo> memberReports = memberDeclarationRepository.findReportDoneList(pageable);
        List<BoReportDoneListDto.Response.ReportInfo> boardReports = roommateBoardDeclarationRepository.findReportDoneList(pageable);

        return Stream.concat(memberReports.stream(), boardReports.stream()).sorted(Comparator.comparing(BoReportDoneListDto.Response.ReportInfo::getCreatedAt).reversed()).toList();
    }

    @Transactional
    public MemberDeclaration reportMember(Member reporter, Member reported, String reason) {
        if (memberDeclarationRepository.existsByReporterIdAndReportedId(reporter.getId(), reported.getId())) {
            throw new BusinessException(MemberErrorCode.DECLARATION_DUPLICATE);
        }

        MemberDeclaration memberDeclaration = MemberDeclaration.builder()
                .reporterId(reporter.getId())
                .reportedId(reported.getId())
                .reason(reason)
                .declarationType(DeclarationType.PENDING)
                .build();

        return memberDeclarationRepository.save(memberDeclaration);
    }

    @Transactional
    public void reportBoard(RoommateBoard roommateBoard, Member member, String reason) {
        roommateBoardDeclarationRepository.findByRoommateBoardIdAndMemberId(roommateBoard.getId(), member.getId())
                .ifPresent(declaration -> {
                    throw new BusinessException(RoommateBoardErrorCode.ROOMMATE_BOARD_DECLARATION_DUPLICATE);
                });

        RoommateBoardDeclaration roommateBoardDeclaration = RoommateBoardDeclaration.builder()
                .memberId(member.getId())
                .roommateBoardId(roommateBoard.getId())
                .reason(reason)
                .declarationType(DeclarationType.PENDING)
                .build();

        roommateBoardDeclarationRepository.save(roommateBoardDeclaration);
    }

    @Transactional
    public void reportHidden(Long id, ReportType type, String reason) {
        switch (type) {
            case BOARD -> roommateBoardDeclarationRepository.findById(id).orElseThrow(() -> new BusinessException(DeclarationErrorCode.DECLARATION_NOT_FOUND)).changeDeclarationType(DeclarationType.HIDDEN);
            case MEMBER -> memberDeclarationRepository.findById(id).orElseThrow(() -> new BusinessException(DeclarationErrorCode.DECLARATION_NOT_FOUND)).changeDeclarationType(DeclarationType.HIDDEN);
        }
    }

    @Transactional
    public void reportNoAction(Long id, ReportType type) {
        switch (type) {
            case BOARD -> roommateBoardDeclarationRepository.findById(id).orElseThrow(() -> new BusinessException(DeclarationErrorCode.DECLARATION_NOT_FOUND)).changeDeclarationType(DeclarationType.NOACTION);
            case MEMBER -> memberDeclarationRepository.findById(id).orElseThrow(() -> new BusinessException(DeclarationErrorCode.DECLARATION_NOT_FOUND)).changeDeclarationType(DeclarationType.NOACTION);
        }
    }

    @Transactional
    public void reportSuspended(Long id, ReportType type, String reason) {
        switch (type) {
            case BOARD -> roommateBoardDeclarationRepository.findById(id).orElseThrow(() -> new BusinessException(DeclarationErrorCode.DECLARATION_NOT_FOUND)).changeDeclarationType(DeclarationType.SUSPENDED, reason);
            case MEMBER -> {
                memberDeclarationRepository.findById(id).orElseThrow(() -> new BusinessException(DeclarationErrorCode.DECLARATION_NOT_FOUND)).changeDeclarationType(DeclarationType.SUSPENDED);
                memberService.findStateByMemberId(id).rejectState(reason);
            }
        }
    }
}
