package org.example.knockin.inquiry.service.impl;

import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.InquiryErrorCode;
import org.example.knockin.inquiry.dto.BoInquiryDetailDto;
import org.example.knockin.inquiry.dto.BoInquiryListDto;
import org.example.knockin.inquiry.dto.InquiryCategoryListDto;
import org.example.knockin.inquiry.dto.InquiryDetailDto;
import org.example.knockin.inquiry.dto.InquiryDto;
import org.example.knockin.inquiry.dto.InquiryListDto;
import org.example.knockin.inquiry.entity.Inquiry;
import org.example.knockin.inquiry.entity.InquiryCategory;
import org.example.knockin.inquiry.entity.InquiryComment;
import org.example.knockin.inquiry.repository.InquiryCategoryRepository;
import org.example.knockin.inquiry.repository.InquiryCommentRepository;
import org.example.knockin.inquiry.repository.InquiryRepository;
import org.example.knockin.inquiry.service.InquiryReplyEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("1:1 문의 서비스 테스트")
class InquiryServiceImplTest {

    @Mock
    private InquiryCategoryRepository inquiryCategoryRepository;

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private InquiryCommentRepository inquiryCommentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private InquiryServiceImpl inquiriesService;

    @Test
    @DisplayName("문의 저장 성공 테스트 (saveInquiry)")
    void saveInquirySuccessTest() {
        // given
        InquiryDto.Request request = new InquiryDto.Request();
        request.setCategoryId(1L);
        request.setTitle("Inquiry Title");
        request.setContents("Inquiry Contents");

        Long memberId = 42L;
        InquiryCategory category = mock(InquiryCategory.class);

        given(inquiryCategoryRepository.findById(1L)).willReturn(Optional.of(category));

        // when
        inquiriesService.saveInquiry(request, memberId);

        // then
        ArgumentCaptor<Inquiry> inquiryCaptor = ArgumentCaptor.forClass(Inquiry.class);
        verify(inquiryRepository).save(inquiryCaptor.capture());
        Inquiry inquiry = inquiryCaptor.getValue();
        assertThat(inquiry.getMemberId()).isEqualTo(memberId);
        assertThat(inquiry.getInquiryCategory()).isSameAs(category);
        assertThat(inquiry.getTitle()).isEqualTo(request.getTitle());
        assertThat(inquiry.getContents()).isEqualTo(request.getContents());
    }

    @Test
    @DisplayName("문의 저장 시 카테고리가 없으면 BusinessException 발생")
    void saveInquiryCategoryNotFoundTest() {
        // given
        InquiryDto.Request request = new InquiryDto.Request();
        request.setCategoryId(1L);

        given(inquiryCategoryRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inquiriesService.saveInquiry(request, 42L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", InquiryErrorCode.INQUIRY_CATEGORY_NOT_FOUND);

        verifyNoInteractions(inquiryRepository);
    }

    @Test
    @DisplayName("문의 저장 비즈니스 로직 성공 테스트 (saveInquiryLogic)")
    void saveInquiryLogicSuccessTest() {
        // given
        Long memberId = 1L;
        InquiryDto.Request request = new InquiryDto.Request();
        request.setCategoryId(1L);
        request.setTitle("Inquiry Title");
        request.setContents("Inquiry Contents");

        InquiryCategory category = mock(InquiryCategory.class);

        given(inquiryCategoryRepository.findById(1L)).willReturn(Optional.of(category));

        // when
        InquiryDto.Response response = inquiriesService.saveInquiryLogic(request, memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
        ArgumentCaptor<Inquiry> inquiryCaptor = ArgumentCaptor.forClass(Inquiry.class);
        verify(inquiryRepository).save(inquiryCaptor.capture());
        assertThat(inquiryCaptor.getValue().getMemberId()).isEqualTo(memberId);
    }

    @Test
    @DisplayName("문의 저장 로직 실행 시 카테고리가 없으면 BusinessException 발생")
    void saveInquiryLogicCategoryNotFoundTest() {
        // given
        Long memberId = 1L;
        InquiryDto.Request request = new InquiryDto.Request();
        request.setCategoryId(1L);
        given(inquiryCategoryRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inquiriesService.saveInquiryLogic(request, memberId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", InquiryErrorCode.INQUIRY_CATEGORY_NOT_FOUND);

        verifyNoInteractions(inquiryRepository);
    }

    @Test
    @DisplayName("삭제되지 않은 문의 카테고리 목록 조회 성공 테스트")
    void findInquiryCategoryListSuccessTest() {
        // given
        InquiryCategory category1 = InquiryCategory.builder().id(1L).title("General").isDeleted(false).build();
        InquiryCategory category2 = InquiryCategory.builder().id(2L).title("Payment").isDeleted(false).build();

        given(inquiryCategoryRepository.findAllByIsDeleted(false)).willReturn(List.of(category1, category2));

        // when
        InquiryCategoryListDto.Response response = inquiriesService.findInquiryCategoryList();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getInquirieCategorys()).hasSize(2);
        assertThat(response.getInquirieCategorys().get(0).getName()).isEqualTo("General");
        assertThat(response.getInquirieCategorys().get(1).getName()).isEqualTo("Payment");
    }

    @Test
    @DisplayName("회원의 문의 목록 조회 성공 테스트")
    void findInquiryListSuccessTest() {
        // given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        InquiryListDto.Response.InquiryItem item = InquiryListDto.Response.InquiryItem.builder()
                .id(100L)
                .title("Inquiry Title")
                .build();

        given(inquiryRepository.findMyInquiryList(false, memberId, pageable)).willReturn(List.of(item));

        // when
        InquiryListDto.Response response = inquiriesService.findInquiryList(pageable, memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getInquiries()).hasSize(1);
        assertThat(response.getInquiries().get(0).getId()).isEqualTo(100L);
        verify(inquiryRepository).findMyInquiryList(false, memberId, pageable);
    }

    @Test
    @DisplayName("회원의 문의 상세 조회 성공 테스트")
    void findInquiryDetailSuccessTest() {
        // given
        Long memberId = 1L;
        Long inquiryId = 100L;
        InquiryDetailDto.Response.InquiryDetail detail = InquiryDetailDto.Response.InquiryDetail.builder()
                .id(inquiryId)
                .title("Title")
                .contents("Contents")
                .build();

        given(inquiryRepository.findMyInquiry(false, memberId, inquiryId)).willReturn(detail);

        // when
        InquiryDetailDto.Response response = inquiriesService.findInquiry(inquiryId, memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getInquirie()).isEqualTo(detail);
        verify(inquiryRepository).findMyInquiry(false, memberId, inquiryId);
    }

    @Test
    @DisplayName("문의 답변 저장 후 문의 작성자와 문의 ID로 답변 이벤트 발행")
    void saveInquiryReplySuccessTest() {
        // given
        Inquiry inquiry = Inquiry.builder().id(100L).memberId(10L).build();
        InquiryComment comment = InquiryComment.builder()
                .inquiry(inquiry)
                .memberId(20L)
                .contents("답변 내용")
                .build();
        InquiryComment savedComment = InquiryComment.builder()
                .id(200L)
                .inquiry(inquiry)
                .memberId(20L)
                .contents("답변 내용")
                .build();
        given(inquiryCommentRepository.save(comment)).willReturn(savedComment);

        // when
        InquiryComment result = inquiriesService.saveInquiryReply(comment);

        // then
        assertThat(result).isSameAs(savedComment);
        assertThat(result.getContents()).isEqualTo("답변 내용");
        InOrder inOrder = inOrder(inquiryCommentRepository, eventPublisher);
        inOrder.verify(inquiryCommentRepository).save(comment);
        inOrder.verify(eventPublisher).publishEvent(new InquiryReplyEvent(10L, 100L));
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    @DisplayName("문의 답변 저장 실패 시 답변 이벤트를 발행하지 않음")
    void saveInquiryReplyFailureDoesNotPublishEventTest() {
        // given
        Inquiry inquiry = Inquiry.builder().id(100L).memberId(10L).build();
        InquiryComment comment = InquiryComment.builder()
                .inquiry(inquiry)
                .memberId(20L)
                .contents("답변 내용")
                .build();
        RuntimeException exception = new RuntimeException("답변 저장 실패");
        given(inquiryCommentRepository.save(comment)).willThrow(exception);

        // when & then
        assertThatThrownBy(() -> inquiriesService.saveInquiryReply(comment))
                .isSameAs(exception);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("문의 ID로 조회 성공 테스트 (findInquiryById)")
    void findInquiryByIdSuccessTest() {
        // given
        Long inquiryId = 100L;
        Inquiry inquiry = Inquiry.builder()
                .id(inquiryId)
                .title("제목")
                .build();
        given(inquiryRepository.findByIdAndIsDeleted(inquiryId, false)).willReturn(Optional.of(inquiry));

        // when
        Inquiry result = inquiriesService.findInquiryById(inquiryId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(inquiryId);
        assertThat(result.getTitle()).isEqualTo("제목");
        verify(inquiryRepository).findByIdAndIsDeleted(inquiryId, false);
    }

    @Test
    @DisplayName("문의 ID로 조회 시 문의가 없으면 BusinessException 발생 (findInquiryById)")
    void findInquiryByIdNotFoundTest() {
        // given
        Long inquiryId = 100L;
        given(inquiryRepository.findByIdAndIsDeleted(inquiryId, false)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inquiriesService.findInquiryById(inquiryId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", InquiryErrorCode.INQUIRY_NOT_FOUND);
    }

    @Test
    @DisplayName("백오피스 문의 목록 조회 성공 테스트 (findBackOfficeInquirieList)")
    void findBackOfficeInquirieListSuccessTest() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        BoInquiryListDto.Response.InquiryItem item = new BoInquiryListDto.Response.InquiryItem();
        item.setId(100L);
        item.setTitle("백오피스 문의 제목");
        BoInquiryListDto.Request request = new BoInquiryListDto.Request();

        given(inquiryRepository.findBackOfficeInquirieList(pageable, request)).willReturn(List.of(item));

        // when
        List<BoInquiryListDto.Response.InquiryItem> result = inquiriesService.findBackOfficeInquirieList(pageable, request);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(100L);
        assertThat(result.get(0).getTitle()).isEqualTo("백오피스 문의 제목");
        verify(inquiryRepository).findBackOfficeInquirieList(pageable, request);
    }

    @Test
    @DisplayName("백오피스 문의 상세 조회 성공 테스트 (findBackOfficeInquirie)")
    void findBackOfficeInquirieSuccessTest() {
        // given
        Long id = 100L;
        BoInquiryDetailDto.Response.InquiryDetail detail = new BoInquiryDetailDto.Response.InquiryDetail();
        detail.setId(id);
        detail.setTitle("백오피스 문의 제목");

        BoInquiryDetailDto.Response.InquiryDetail.Reply reply = new BoInquiryDetailDto.Response.InquiryDetail.Reply();
        reply.setId(200L);
        reply.setContents("답변 내용");

        given(inquiryRepository.findBackOfficeInquirie(id)).willReturn(detail);
        given(inquiryRepository.findBackOfficeInquirieReply(id)).willReturn(List.of(reply));

        // when
        BoInquiryDetailDto.Response.InquiryDetail result = inquiriesService.findBackOfficeInquirie(id);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getTitle()).isEqualTo("백오피스 문의 제목");
        assertThat(result.getReply()).hasSize(1);
        assertThat(result.getReply().get(0).getId()).isEqualTo(200L);
        assertThat(result.getReply().get(0).getContents()).isEqualTo("답변 내용");
        verify(inquiryRepository).findBackOfficeInquirie(id);
        verify(inquiryRepository).findBackOfficeInquirieReply(id);
    }
}
