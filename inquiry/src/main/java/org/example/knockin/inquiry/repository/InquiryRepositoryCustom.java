package org.example.knockin.inquiry.repository;

import org.example.knockin.inquiry.dto.BoInquiryDetailDto;
import org.example.knockin.inquiry.dto.BoInquiryListDto;
import org.example.knockin.inquiry.dto.InquiryDetailDto;
import org.example.knockin.inquiry.dto.InquiryListDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InquiryRepositoryCustom {
    List<InquiryListDto.Response.InquiryItem> findMyInquiryList(Boolean isDeleted, Long memberId, Pageable pageable);
    InquiryDetailDto.Response.InquiryDetail findMyInquiry(Boolean isDeleted, Long memberId, Long inquiryId);
    List<BoInquiryListDto.Response.InquiryItem> findBackOfficeInquirieList(Pageable pageable, BoInquiryListDto.Request request);
    BoInquiryDetailDto.Response.InquiryDetail findBackOfficeInquirie(Long id);
    List<BoInquiryDetailDto.Response.InquiryDetail.Reply> findBackOfficeInquirieReply(Long id);
}