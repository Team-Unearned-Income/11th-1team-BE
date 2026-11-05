package org.example.knockin.inquiry.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.knockin.inquiry.dto.BoInquiryDetailDto;
import org.example.knockin.inquiry.dto.BoInquiryListDto;
import org.example.knockin.inquiry.dto.InquiryCategoryListDto;
import org.example.knockin.inquiry.dto.InquiryDetailDto;
import org.example.knockin.inquiry.dto.InquiryDto;
import org.example.knockin.inquiry.dto.InquiryListDto;
import org.example.knockin.inquiry.entity.Inquiry;
import org.example.knockin.inquiry.entity.InquiryCategory;
import org.example.knockin.inquiry.entity.InquiryComment;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.InquiryErrorCode;
import org.example.knockin.inquiry.repository.InquiryCategoryRepository;
import org.example.knockin.inquiry.repository.InquiryCommentRepository;
import org.example.knockin.inquiry.repository.InquiryRepository;
import org.example.knockin.inquiry.service.InquiryReplyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InquirieServiceImpl {
    private final InquiryCategoryRepository inquiryCategoryRepository;
    private final InquiryRepository inquiryRepository;
    private final InquiryCommentRepository inquiryCommentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void saveInquiry(InquiryDto.Request request, Long memberId) {
        InquiryCategory inquiryCategory = inquiryCategoryRepository.findById(request.getCategoryId()).orElseThrow(() -> new BusinessException(InquiryErrorCode.INQUIRY_CATEGORY_NOT_FOUND));
        inquiryRepository.save(Inquiry.builder().inquiryCategory(inquiryCategory).memberId(memberId).title(request.getTitle()).contents(request.getContents()).build());
    }

    @Transactional
    public InquiryDto.Response saveInquiryLogic(InquiryDto.Request request, Long memberId) {
        saveInquiry(request, memberId);
        return InquiryDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    public InquiryCategoryListDto.Response findInquirieCategoryList() {
        List<InquiryCategoryListDto.Response.Category> categoryList = inquiryCategoryRepository.findAllByIsDeleted(false).stream().map(item -> InquiryCategoryListDto.Response.Category.builder().id(item.getId()).name(item.getTitle()).build()).toList();
        return InquiryCategoryListDto.Response.builder().inquirieCategorys(categoryList).build();
    }

    public InquiryListDto.Response findInquirieList(Pageable pageable, Long memberId) {
        List<InquiryListDto.Response.InquiryItem> inquiryItemList = inquiryRepository.findMyInquiryList(false, memberId, pageable);
        return InquiryListDto.Response.builder().inquiries(inquiryItemList).build();
    }

    public InquiryDetailDto.Response findInquirie(Long inquiryId, Long memberId) {
        return InquiryDetailDto.Response.builder().inquirie(inquiryRepository.findMyInquiry(false, memberId, inquiryId)).build();
    }

    @Transactional
    public InquiryComment saveInquirieReply(InquiryComment inquiryComment) {
        InquiryComment inquiryCommentEntity = inquiryCommentRepository.save(inquiryComment);

        Long memberId = inquiryCommentEntity.getInquiry().getMemberId();
        Long inquiryId = inquiryCommentEntity.getInquiry().getId();
        eventPublisher.publishEvent(new InquiryReplyEvent(memberId, inquiryId));

        return inquiryCommentEntity;
    }

    public Inquiry findInquiryById(Long inquiryId) {
        return inquiryRepository.findByIdAndIsDeleted(inquiryId, false).orElseThrow(() -> new BusinessException(InquiryErrorCode.INQUIRY_NOT_FOUND));
    }

    public List<BoInquiryListDto.Response.InquiryItem> findBackOfficeInquirieList(Pageable pageable, BoInquiryListDto.Request request) {
        return inquiryRepository.findBackOfficeInquirieList(pageable, request);
    }

    public BoInquiryDetailDto.Response.InquiryDetail findBackOfficeInquirie(Long id) {
        BoInquiryDetailDto.Response.InquiryDetail inquiryDetail = inquiryRepository.findBackOfficeInquirie(id);
        inquiryDetail.setReply(inquiryRepository.findBackOfficeInquirieReply(id));
        return inquiryDetail;
    }
}
