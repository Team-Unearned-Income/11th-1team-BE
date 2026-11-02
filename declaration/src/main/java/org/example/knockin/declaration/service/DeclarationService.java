package org.example.knockin.declaration.service;

import java.util.List;
import org.example.knockin.declaration.dto.BoReportDoneListDto;
import org.example.knockin.declaration.dto.BoReportWaitListDto.Response.ReportInfo;
import org.example.knockin.declaration.entity.MemberDeclaration;
import org.example.knockin.global.util.ReportType;
import org.springframework.data.domain.Pageable;

public interface DeclarationService {
    List<ReportInfo> findReportWaitList(Pageable pageable);

    List<BoReportDoneListDto.Response.ReportInfo> findReportDoneList(Pageable pageable);

    MemberDeclaration reportMember(Long reporterId, Long reportedId, String reason);

    void reportBoard(Long boardId, Long memberId, String reason);

    void reportHidden(Long id, ReportType type, String reason);

    void reportNoAction(Long id, ReportType type);

    void reportSuspended(Long id, ReportType type, String reason);
}
