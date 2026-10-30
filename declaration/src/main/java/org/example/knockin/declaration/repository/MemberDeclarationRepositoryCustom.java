package org.example.knockin.declaration.repository;

import org.example.knockin.declaration.dto.BoReportDoneListDto;
import org.example.knockin.declaration.dto.BoReportWaitListDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MemberDeclarationRepositoryCustom {
    List<BoReportWaitListDto.Response.ReportInfo> findReportWaitList(Pageable pageable);
    List<BoReportDoneListDto.Response.ReportInfo> findReportDoneList(Pageable pageable);
}
