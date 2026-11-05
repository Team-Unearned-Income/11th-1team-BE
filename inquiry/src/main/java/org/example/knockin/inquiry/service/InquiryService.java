package org.example.knockin.inquiry.service;

import java.util.List;
import org.example.knockin.inquiry.dto.BoInquiryDetailDto;
import org.example.knockin.inquiry.dto.BoInquiryListDto;
import org.example.knockin.inquiry.dto.BoInquiryListDto.Response.InquiryItem;
import org.example.knockin.inquiry.dto.InquiryCategoryListDto;
import org.example.knockin.inquiry.dto.InquiryDetailDto;
import org.example.knockin.inquiry.dto.InquiryDto;
import org.example.knockin.inquiry.dto.InquiryListDto;
import org.example.knockin.inquiry.entity.Inquiry;
import org.example.knockin.inquiry.entity.InquiryComment;
import org.springframework.data.domain.Pageable;

public interface InquiryService {
    void saveInquiry(InquiryDto.Request request, Long memberId);

    InquiryDto.Response saveInquiryLogic(InquiryDto.Request request, Long memberId);

    InquiryCategoryListDto.Response findInquiryCategoryList();

    InquiryListDto.Response findInquiryList(Pageable pageable, Long memberId);

    InquiryDetailDto.Response findInquiry(Long inquiryId, Long memberId);

    InquiryComment saveInquiryReply(InquiryComment inquiryComment);

    Inquiry findInquiryById(Long inquiryId);

    List<InquiryItem> findBackOfficeInquirieList(Pageable pageable, BoInquiryListDto.Request request);

    BoInquiryDetailDto.Response.InquiryDetail findBackOfficeInquirie(Long id);
}
