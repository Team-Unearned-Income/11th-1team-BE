package org.example.knockin.service.impl;

import org.example.knockin.alarm.service.impl.NotificationServiceImpl;
import org.example.knockin.meta.dto.BoNoticeListDto;
import org.example.knockin.meta.dto.NoticeDetailDto;
import org.example.knockin.alarm.entity.Notification;
import org.example.knockin.alarm.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("공지사항(Notification) 서비스 테스트")
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    @DisplayName("공지사항 등록 성공 테스트 (saveNotification)")
    void saveNotificationSuccessTest() {
        // given
        Notification notification = Notification.builder()
                .title("제목")
                .contents("내용")
                .build();
        given(notificationRepository.save(any(Notification.class))).willReturn(notification);

        // when
        Notification result = notificationService.saveNotification(notification);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("제목");
        assertThat(result.getContents()).isEqualTo("내용");
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("공지사항 목록 조회 성공 테스트 (findNotificationList)")
    void findNotificationListSuccessTest() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        BoNoticeListDto.Response.NoticeItem noticeItem = BoNoticeListDto.Response.NoticeItem.builder()
                .id(1L)
                .title("제목")
                .writer("작성자")
                .build();
        given(notificationRepository.findBoNotificationsByIsDeleted(false, pageable))
                .willReturn(List.of(noticeItem));

        // when
        List<BoNoticeListDto.Response.NoticeItem> result = notificationService.findBoNotificationList(pageable);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getTitle()).isEqualTo("제목");
        assertThat(result.get(0).getWriter()).isEqualTo("작성자");
        verify(notificationRepository).findBoNotificationsByIsDeleted(false, pageable);
    }

    @Test
    @DisplayName("공지사항 ID로 조회 성공 테스트 (findNotificationById)")
    void findNotificationByIdSuccessTest() {
        // given
        Long id = 1L;
        Notification notification = Notification.builder()
                .id(id)
                .title("제목")
                .contents("내용")
                .build();
        given(notificationRepository.findByIdAndIsDeleted(id, false)).willReturn(notification);

        // when
        Notification result = notificationService.findNotificationById(id);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getTitle()).isEqualTo("제목");
        verify(notificationRepository).findByIdAndIsDeleted(id, false);
    }

    @Test
    @DisplayName("공지사항 상세 조회 Dto 반환 성공 테스트 (findNotification)")
    void findNotificationSuccessTest() {
        // given
        Long id = 1L;
        NoticeDetailDto.Response response = new NoticeDetailDto.Response();
        NoticeDetailDto.Response.NoticeDetail noticeDetail = new NoticeDetailDto.Response.NoticeDetail();
        noticeDetail.setId(id);
        noticeDetail.setTitle("제목");
        noticeDetail.setContents("내용");
        noticeDetail.setWriter("작성자");
        response.setNotice(noticeDetail);

        given(notificationRepository.findNotificationByIsDeleted(false, id)).willReturn(noticeDetail);

        // when
        NoticeDetailDto.Response result = notificationService.findNotification(id);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getNotice().getId()).isEqualTo(id);
        assertThat(result.getNotice().getTitle()).isEqualTo("제목");
        assertThat(result.getNotice().getContents()).isEqualTo("내용");
        assertThat(result.getNotice().getWriter()).isEqualTo("작성자");
        verify(notificationRepository).findNotificationByIsDeleted(false, id);
    }
}
