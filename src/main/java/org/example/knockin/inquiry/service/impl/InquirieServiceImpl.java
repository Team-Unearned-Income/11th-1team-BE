package org.example.knockin.inquiry.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.knockin.meta.service.impl.AlarmServiceImpl;
import org.example.knockin.meta.service.impl.PushNotificationServiceImpl;
import org.example.knockin.meta.entity.Alarm;
import org.example.knockin.meta.entity.AlarmSettingType;
import org.example.knockin.meta.entity.AlarmType;
import org.example.knockin.inquiry.dto.BoInquiryDetailDto;
import org.example.knockin.inquiry.dto.BoInquiryListDto;
import org.example.knockin.inquiry.dto.InquiryCategoryListDto;
import org.example.knockin.inquiry.dto.InquiryDetailDto;
import org.example.knockin.inquiry.dto.InquiryDto;
import org.example.knockin.inquiry.dto.InquiryListDto;
import org.example.knockin.inquiry.entity.Inquiry;
import org.example.knockin.global.entity.InquiryAlarmTemplate;
import org.example.knockin.inquiry.entity.InquiryCategory;
import org.example.knockin.inquiry.entity.InquiryComment;
import org.example.knockin.member.entity.Member;
import org.example.knockin.global.exception.AuthErrorCode;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.InquiryErrorCode;
import org.example.knockin.inquiry.repository.InquiryCategoryRepository;
import org.example.knockin.inquiry.repository.InquiryCommentRepository;
import org.example.knockin.inquiry.repository.InquiryRepository;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InquirieServiceImpl {
    private final MemberServiceImpl memberService;
    private final InquiryCategoryRepository inquiryCategoryRepository;
    private final InquiryRepository inquiryRepository;
    private final InquiryCommentRepository inquiryCommentRepository;
    private final AlarmServiceImpl alarmService;
    private final PushNotificationServiceImpl pushNotificationService;

    @Transactional
    public void saveInquiry(InquiryDto.Request request, Member member) {
        InquiryCategory inquiryCategory = inquiryCategoryRepository.findById(request.getCategoryId()).orElseThrow(() -> new BusinessException(InquiryErrorCode.INQUIRY_CATEGORY_NOT_FOUND));
        inquiryRepository.save(Inquiry.builder().inquiryCategory(inquiryCategory).memberId(member.getId()).title(request.getTitle()).contents(request.getContents()).build());
    }

    @Transactional
    public InquiryDto.Response saveInquiryLogic(InquiryDto.Request request, Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        saveInquiry(request, member);
        return InquiryDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    public InquiryCategoryListDto.Response findInquirieCategoryList() {
        List<InquiryCategoryListDto.Response.Category> categoryList = inquiryCategoryRepository.findAllByIsDeleted(false).stream().map(item -> InquiryCategoryListDto.Response.Category.builder().id(item.getId()).name(item.getTitle()).build()).toList();
        return InquiryCategoryListDto.Response.builder().inquirieCategorys(categoryList).build();
    }

    public InquiryListDto.Response findInquirieList(Pageable pageable, Long memberId) {
        memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        List<InquiryListDto.Response.InquiryItem> inquiryItemList = inquiryRepository.findMyInquiryList(false, memberId, pageable);
        return InquiryListDto.Response.builder().inquiries(inquiryItemList).build();
    }

    public InquiryDetailDto.Response findInquirie(Long inquiryId, Long memberId) {
        memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        return InquiryDetailDto.Response.builder().inquirie(inquiryRepository.findMyInquiry(false, memberId, inquiryId)).build();
    }

    @Transactional
    public InquiryComment saveInquirieReply(InquiryComment inquiryComment) {
        InquiryComment inquiryCommentEntity = inquiryCommentRepository.save(inquiryComment);

        Long memberId = inquiryCommentEntity.getInquiry().getMemberId();
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        Alarm alarm = Alarm.builder()
                .title(InquiryAlarmTemplate.INQUIRIE_REPLY.formatTitle())
                .contents(InquiryAlarmTemplate.INQUIRIE_REPLY.formatContents())
                .isRead(false)
                .member(member)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .type(AlarmType.DEFAULT)
                .build();

        alarmService.sendToClient(member.getId(), InquiryAlarmTemplate.INQUIRIE_REPLY.name(), alarm);
        pushNotificationService.send(member, AlarmSettingType.NOTIFICATION, InquiryAlarmTemplate.INQUIRIE_REPLY.formatTitle(), InquiryAlarmTemplate.INQUIRIE_REPLY.formatContents(),InquiryAlarmTemplate.INQUIRIE_REPLY.formatDeepLink());

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
