package org.example.knockin.util.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.knockin.agreement.dto.BoTermsDetailDto;
import org.example.knockin.agreement.dto.BoTermsDto;
import org.example.knockin.agreement.dto.BoTermsListDto;
import org.example.knockin.agreement.dto.BoTypeTermsDto;
import org.example.knockin.agreement.dto.BoTypeTermsListDto;
import org.example.knockin.agreement.service.impl.AgreementServiceImpl;
import org.example.knockin.board.dto.BoBoardDeleteDto;
import org.example.knockin.board.dto.BoBoardDetailDto;
import org.example.knockin.board.dto.BoBoardListDto;
import org.example.knockin.member.dto.BoMemberAuthDto;
import org.example.knockin.member.dto.BoMemberCancelDto;
import org.example.knockin.member.dto.BoMemberDetailDto;
import org.example.knockin.member.dto.BoMemberListDto;
import org.example.knockin.member.dto.BoMemberUnCancelDto;
import org.example.knockin.meta.dto.BoNoticeDetailDto;
import org.example.knockin.meta.dto.BoNoticeDto;
import org.example.knockin.meta.dto.BoNoticeListDto;
import org.example.knockin.meta.service.impl.NotificationServiceImpl;
import org.example.knockin.verification.dto.BoVerificationApproveListDto;
import org.example.knockin.verification.dto.BoVerificationCancelListDto;
import org.example.knockin.verification.dto.BoVerificationDto;
import org.example.knockin.verification.dto.BoVerificationWaitingDetailDto;
import org.example.knockin.verification.dto.BoVerificationWaitingListDto;
import org.example.knockin.verification.service.impl.AuthenticationServiceImpl;
import org.example.knockin.board.service.impl.RoommateBoardServiceImpl;
import org.example.knockin.declaration.dto.BoReportDoneListDto;
import org.example.knockin.declaration.dto.BoReportHiddenDto;
import org.example.knockin.declaration.dto.BoReportNoActionDto;
import org.example.knockin.declaration.dto.BoReportSuspendedDto;
import org.example.knockin.declaration.dto.BoReportWaitListDto;
import org.example.knockin.declaration.service.impl.DeclarationServiceImpl;
import org.example.knockin.agreement.entity.Agreement;
import org.example.knockin.agreement.entity.AgreementType;
import org.example.knockin.meta.entity.Notification;
import org.example.knockin.member.service.impl.MemberServiceImpl;
import org.example.knockin.meta.entity.File;
import org.example.knockin.meta.entity.FileType;
import org.example.knockin.inquiry.dto.BoInquiryDetailDto;
import org.example.knockin.inquiry.dto.BoInquiryListDto;
import org.example.knockin.inquiry.dto.BoInquiryReplyDto;
import org.example.knockin.inquiry.entity.Inquiry;
import org.example.knockin.inquiry.entity.InquiryComment;
import org.example.knockin.life.service.impl.LifeStyleServiceImpl;
import org.example.knockin.life.dto.BoLifeStylePatternDetailDto;
import org.example.knockin.life.dto.BoLifeStylePatternDto;
import org.example.knockin.life.dto.BoLifeStylePatternListDto;
import org.example.knockin.life.entity.LifePattern;
import org.example.knockin.life.entity.LifePatternFile;
import org.example.knockin.life.entity.LifePatternInformation;
import org.example.knockin.member.entity.Member;
import org.example.knockin.member.entity.MemberState;
import org.example.knockin.room.dto.BoRoomAddOptionDetailDto;
import org.example.knockin.room.dto.BoRoomAddOptionDto;
import org.example.knockin.room.dto.BoRoomAddOptionListDto;
import org.example.knockin.room.dto.BoRoomTypeDetailDto;
import org.example.knockin.room.dto.BoRoomTypeDto;
import org.example.knockin.room.dto.BoRoomTypeListDto;
import org.example.knockin.room.entity.RoomExtraOption;
import org.example.knockin.room.entity.RoomExtraOptionFile;
import org.example.knockin.room.entity.RoomType;
import org.example.knockin.room.entity.RoomTypeFile;
import org.example.knockin.global.exception.AuthErrorCode;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.FileErrorCode;
import org.example.knockin.inquiry.service.impl.InquirieServiceImpl;
import org.example.knockin.room.service.impl.RoomExtraOptionServiceImpl;
import org.example.knockin.room.service.impl.RoomTypeServiceImpl;
import org.example.knockin.meta.service.FileService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BackOfficeServiceImpl {
    private final AgreementServiceImpl agreementService;
    private final RoomTypeServiceImpl roomTypeService;
    private final LifeStyleServiceImpl lifeStyleService;
    private final AuthenticationServiceImpl authenticationService;
    private final NotificationServiceImpl notificationService;
    private final MemberServiceImpl memberService;
    private final InquirieServiceImpl inquirieService;
    private final DeclarationServiceImpl declarationService;
    private final RoommateBoardServiceImpl roommateBoardService;
    private final RoomExtraOptionServiceImpl roomExtraOptionService;
    private final FileService fileService;

    @Transactional
    public BoTermsDto.Response saveTerms(BoTermsDto.Request request) {
        AgreementType agreementType = agreementService.findAgreementTypeById(request.getAgreementTypeId());
        agreementService.saveAgreement(Agreement.builder().title(request.getTitle()).contents(request.getContents()).isRequired(request.getIsRequired()).type(agreementType).build());
        return BoTermsDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public BoTermsDto.Response modifyTerms(BoTermsDto.Request request, Long termsId) {
        AgreementType type = agreementService.findAgreementType(termsId);
        agreementService.modifyTemporaryAgreement(Agreement.builder().title(request.getTitle()).contents(request.getContents()).isRequired(request.getIsRequired()).type(type).isDeleted(false).build());
        return BoTermsDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    public BoTermsListDto.Response findTermsList(Pageable pageable, BoTermsListDto.Request request) {
        List<BoTermsListDto.Response.TermsItem> termsItemList = agreementService.findAgreementList(pageable, request.getAgreementTypeId());
        return BoTermsListDto.Response.builder().terms(termsItemList).build();
    }

    public BoTermsDetailDto.Response findTerms(Long termsId) {
        Agreement agreement = agreementService.findAgreement(termsId);
        return BoTermsDetailDto.Response.builder().id(agreement.getId()).title(agreement.getTitle()).contents(agreement.getContents()).createAt(agreement.getCreatedAt()).build();
    }

    @Transactional
    public BoTermsDto.Response deleteTerms(Long termsId) {
        agreementService.deleteAgreement(termsId);
        return BoTermsDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public BoTermsDto.Response modifyLastTerms(BoTermsDto.Request request, Long termsId) {
        agreementService.modifyAgreement(Agreement.builder().title(request.getTitle()).contents(request.getContents()).isRequired(request.getIsRequired()).build(), termsId);
        return BoTermsDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public BoRoomTypeDto.Response saveRoomType(BoRoomTypeDto.Request request, MultipartFile file) {
        try {
            RoomType roomType = roomTypeService.saveRoomType(RoomType.builder().name(request.getName()).build());
            if (file != null && !file.isEmpty()) {
                File fileEntity = fileService.save(file, FileType.ETC);
                roomTypeService.saveRoomTypeFile(RoomTypeFile.builder().roomType(roomType).file(fileEntity).build());
            }
            return BoRoomTypeDto.Response.builder().updatedAt(LocalDateTime.now()).build();
        } catch (IOException e) {
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public BoRoomTypeListDto.Response findRoomTypeList(Pageable pageable) {
        List<BoRoomTypeListDto.Response.RoomTypeItem> roomTypeItemList = roomTypeService.findRoomTypeList(pageable).stream().map(item ->
                BoRoomTypeListDto.Response.RoomTypeItem.builder().id(item.getId()).name(item.getName()).build()).toList();
        return BoRoomTypeListDto.Response.builder().roomType(roomTypeItemList).build();
    }

    @Transactional
    public BoRoomTypeDto.Response modifyRoomType(BoRoomTypeDto.Request request, Long roomTypeId, MultipartFile file) {
        try {
            RoomType roomType = roomTypeService.findRoomType(roomTypeId);
            roomTypeService.modifyRoomType(RoomType.builder().name(request.getName()).build(), roomTypeId);

            if(file != null && !file.isEmpty()) {
                File fileEntity = fileService.save(file, FileType.ETC);
                RoomTypeFile roomTypeFile = roomTypeService.findRoomTypeFile(roomType);
                if(roomTypeFile != null) {
                    roomTypeFile.modifyFile(fileEntity);
                } else {
                    roomTypeService.saveRoomTypeFile(RoomTypeFile.builder()
                            .file(fileEntity)
                            .roomType(roomType)
                            .build());
                }
            }
        } catch (IOException e) {
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_FAILED);
        }

        return BoRoomTypeDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public BoRoomTypeDto.Response deleteRoomType(Long roomTypeId) {
        roomTypeService.deleteRoomType(roomTypeId);
        return BoRoomTypeDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    public BoRoomTypeDetailDto.Response findRoomType(Long roomTypeId) {
        RoomType roomType = roomTypeService.findRoomType(roomTypeId);
        RoomTypeFile roomTypeFile = roomTypeService.findRoomTypeFile(roomType);
        String image = (roomTypeFile != null && roomTypeFile.getFile() != null) ? roomTypeFile.getFile().getSavedFileName() : null;
        return BoRoomTypeDetailDto.Response.builder().id(roomType.getId()).name(roomType.getName()).image(image).build();
    }

    @Transactional
    public BoLifeStylePatternDto.Response saveLifeStylePattern(BoLifeStylePatternDto.Request request, MultipartFile file) {
        try {
            LifePattern lifePattern = lifeStyleService.saveLifePattern(LifePattern.builder().name(request.getName()).lifePatternDescription(request.getLifePatternDescription()).preferenceDescription(request.getPreferenceDescription()).dtype(request.getType()).sort(request.getSort()).build());
            List<LifePatternInformation> lifePatternInformationList = request.getDetails().stream().map(item ->
                    LifePatternInformation.builder().lifePattern(lifePattern).dvalue(item.getValues()).description(item.getDescription()).build()).toList();
            lifeStyleService.saveLifePatternInformation(lifePatternInformationList);

            if (file != null && !file.isEmpty()) {
                File fileEntity = fileService.save(file, FileType.ETC);
                lifeStyleService.saveLifePatternFile(LifePatternFile.builder().lifePattern(lifePattern).fileId(fileEntity.getId()).build());
            }
        } catch (IOException e) {
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_FAILED);
        }

        return BoLifeStylePatternDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    public BoLifeStylePatternListDto.Response findLifeStylePatternList(Pageable pageable) {
        return lifeStyleService.findLifeStylePatternList(pageable);
    }

    public BoLifeStylePatternDetailDto.Response findLifeStylePattern(Long patternId) {
        return lifeStyleService.findLifeStylePattern(patternId);
    }

    @Transactional
    public BoLifeStylePatternDto.Response modifyLifeStylePattern(BoLifeStylePatternDto.Request request, Long patternId, MultipartFile file) {
        try {
            LifePattern lifePattern = lifeStyleService.findLifeStyle(patternId);
            List<LifePatternInformation> existingList = lifeStyleService.findLifeInformationByPattern(lifePattern);
            List<BoLifeStylePatternDto.Request.DetailItem> details = request.getDetails();

            if (details != null) {
                int existingSize = existingList.size();
                int detailSize = details.size();

                for (int i = 0; i < detailSize; i++) {
                    BoLifeStylePatternDto.Request.DetailItem detail = details.get(i);
                    if (i < existingSize) {
                        LifePatternInformation existing = existingList.get(i);
                        existing.modifyLifePatternInformation(detail.getValues(), detail.getDescription());
                    } else {
                        lifeStyleService.saveLifeInformation(LifePatternInformation.builder().lifePattern(lifePattern).dvalue(detail.getValues()).description(detail.getDescription()).build());
                    }
                }

                if (existingSize > detailSize) {
                    existingList.subList(detailSize, existingSize).forEach(lifeStyleService::deleteLifeInformation);
                }
            }

            lifePattern.modifyLifePattern(request.getName(), request.getType(), request.getSort(), request.getLifePatternDescription(), request.getPreferenceDescription());

            if(file != null && !file.isEmpty()) {
                File fileEntity = fileService.save(file, FileType.ETC);
                LifePatternFile lifePatternFile = lifeStyleService.findLifeStyleFile(lifePattern);
                if(lifePatternFile != null) {
                    lifePatternFile.modifyFileId(fileEntity.getId());
                } else {
                    lifeStyleService.saveLifePatternFile(LifePatternFile.builder().fileId(fileEntity.getId()).lifePattern(lifePattern).build());
                }
            }
        } catch (IOException e) {
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_FAILED);
        }

        return BoLifeStylePatternDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public BoLifeStylePatternDto.Response deleteLifeStylePattern(Long patternId) {
        lifeStyleService.deleteLifePattern(patternId);
        return BoLifeStylePatternDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    public BoVerificationApproveListDto.Response findVerificationApproves(Pageable pageable) {
        return BoVerificationApproveListDto.Response.builder().employeeAuth(authenticationService.findVerificationApproves(pageable)).build();
    }

    public BoVerificationCancelListDto.Response findVerificationCancels(Pageable pageable) {
        return BoVerificationCancelListDto.Response.builder().employeeAuth(authenticationService.findVerificationCancels(pageable)).build();
    }

    public BoVerificationWaitingListDto.Response findVerificationsList(Pageable pageable) {
        return BoVerificationWaitingListDto.Response.builder().employeeAuth(authenticationService.findVerificationsList(pageable)).build();
    }

    public BoVerificationWaitingDetailDto.Response findVerifications(Long id) {
        return authenticationService.findVerifications(id);
    }

    @Transactional
    public BoVerificationDto.Response saveVerifications(Long id) {
        authenticationService.saveVerifications(id);
        return BoVerificationDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public BoVerificationDto.Response deleteVerifications(Long id, String rejectReason) {
        authenticationService.deleteVerifications(id, rejectReason);
        return BoVerificationDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public BoNoticeDto.Response saveNotice(BoNoticeDto.Request request, Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        notificationService.saveNotification(Notification.builder().member(member).title(request.getTitle()).contents(request.getContents()).build());
        return BoNoticeDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    public BoNoticeListDto.Response findNoticeList(Pageable pageable) {
        return BoNoticeListDto.Response.builder().notices(notificationService.findBoNotificationList(pageable)).build();
    }

    public BoNoticeDetailDto.Response findNotice(Long id) {
        return notificationService.findBoNotification(id);
    }

    @Transactional
    public BoNoticeDto.Response modifyNotice(BoNoticeDto.Request request, Long id, Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        Notification notification = notificationService.findNotificationById(id);
        notification.modifyNotification(request, member);
        return BoNoticeDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public BoNoticeDto.Response deleteNotice(Long id, Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        Notification notification = notificationService.findNotificationById(id);
        notification.deleteNotification(member);
        return BoNoticeDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public BoInquiryReplyDto.Response saveInquiryReply(BoInquiryReplyDto.Request request, Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        Inquiry inquiry = inquirieService.findInquiryById(request.getInquirieId());
        inquirieService.saveInquirieReply(InquiryComment.builder().memberId(member.getId()).inquiry(inquiry).contents(request.getContents()).build());
        return BoInquiryReplyDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    public BoInquiryListDto.Response findInquirieList(Pageable pageable, BoInquiryListDto.Request request) {
        List<BoInquiryListDto.Response.InquiryItem> inquiryItemList = inquirieService.findBackOfficeInquirieList(pageable, request);
        return BoInquiryListDto.Response.builder().inquiries(inquiryItemList).build();
    }

    public BoInquiryDetailDto.Response findInquirie(Long id) {
        return BoInquiryDetailDto.Response.builder().inquirie(inquirieService.findBackOfficeInquirie(id)).build();
    }

    public BoMemberListDto.Response findMemberList(Pageable pageable, BoMemberListDto.Request request) {
        return memberService.findBackOfficeMemberList(pageable, request);
    }

    public BoMemberDetailDto.Response findMember(Long id) {
        memberService.findById(id).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        return memberService.findBackOfficeMember(id);
    }

    @Transactional
    public BoMemberCancelDto.Response deleteMember(Long id, BoMemberCancelDto.Request request) {
        Member member = memberService.findById(id).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        memberService.setMemberState(member, MemberState.INACTIVE, request.getRejectReason());
        return BoMemberCancelDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public BoMemberUnCancelDto.Response unDeleteMember(Long id) {
        Member member = memberService.findById(id).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        memberService.setMemberState(member, MemberState.ACTIVE, "");
        return BoMemberUnCancelDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public BoMemberAuthDto.Response authMember(Long id, BoMemberAuthDto.Request request) {
        Member member = memberService.findById(id).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
        memberService.setMemberAuth(member, request.getMemberRole());
        return BoMemberAuthDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    public BoReportWaitListDto.Response findReportWaitList(Pageable pageable) {
        return BoReportWaitListDto.Response.builder().reportInfoList(declarationService.findReportWaitList(pageable)).build();
    }

    public BoReportDoneListDto.Response findReportDoneList(Pageable pageable) {
        return BoReportDoneListDto.Response.builder().reportInfoList(declarationService.findReportDoneList(pageable)).build();
    }

    @Transactional
    public BoReportHiddenDto.Response reportHidden(BoReportHiddenDto.Request request) {
        declarationService.reportHidden(request.getId(), request.getType(), request.getReason());
        return BoReportHiddenDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public BoReportNoActionDto.Response reportNoAction(BoReportNoActionDto.Request request) {
        declarationService.reportNoAction(request.getId(), request.getType());
        return BoReportNoActionDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public BoReportSuspendedDto.Response reportSuspended(BoReportSuspendedDto.Request request) {
        declarationService.reportSuspended(request.getId(), request.getType(), request.getReason());
        return BoReportSuspendedDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    public BoBoardListDto.Response findBoardList(Pageable pageable, BoBoardListDto.Request request) {
        return BoBoardListDto.Response.builder().boardInfoList(roommateBoardService.findBackOfficeBoardList(pageable, request)).build();
    }

    public BoBoardDetailDto.Response findBoard(Long id) {
        return roommateBoardService.findBackOffcieBoard(id);
    }

    @Transactional
    public BoBoardDeleteDto.Response deleteBoard(Long id, BoBoardDeleteDto.Request request) {
        roommateBoardService.deleteBackOfficeBoard(id, request.getRejectReason());
        return BoBoardDeleteDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public BoBoardDeleteDto.Response recoverDeleteBoard(Long id) {
        roommateBoardService.recoverDeleteBoard(id);
        return BoBoardDeleteDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    public BoTypeTermsListDto.Response findTypeTermsList() {
        return BoTypeTermsListDto.Response.builder().termTypes(agreementService.findTypeTermsList()).build();
    }

    @Transactional
    public BoTypeTermsDto.Response modifyTermType(Long termTypeId, BoTypeTermsDto.Request request) {
        agreementService.findAgreementTypeById(termTypeId).modifyAgreementType(request.getTitle());
        return BoTypeTermsDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public BoTypeTermsDto.Response saveTermType(BoTypeTermsDto.Request request) {
        agreementService.saveTermType(AgreementType.builder().name(request.getTitle()).isDeleted(false).build());
        return BoTypeTermsDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public BoTypeTermsDto.Response deleteTermType(Long termTypeId) {
        agreementService.deleteTermType(termTypeId);
        return BoTypeTermsDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public BoRoomAddOptionDto.Response saveRoomAddOptions(BoRoomAddOptionDto.Request request, MultipartFile file) {
        try {
            RoomExtraOption roomExtraOption = roomExtraOptionService.saveRoomExtraOption(RoomExtraOption.builder().name(request.getName()).build());
            if (file != null && !file.isEmpty()) {
                File fileEntity = fileService.save(file, FileType.ETC);
                roomExtraOptionService.saveRoomExtraOptionFile(RoomExtraOptionFile.builder().roomExtraOption(roomExtraOption).file(fileEntity).build());
            }
            return BoRoomAddOptionDto.Response.builder().updatedAt(LocalDateTime.now()).build();
        } catch (IOException e) {
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public BoRoomAddOptionListDto.Response findRoomAddOptionsList(Pageable pageable) {
        List<BoRoomAddOptionListDto.Response.RoomAddOptionItem> roomAddOptionItemList = roomExtraOptionService.findRoomExtraOptionList(pageable).stream().map(item ->
                BoRoomAddOptionListDto.Response.RoomAddOptionItem.builder().id(item.getId()).name(item.getName()).build()).toList();
        return BoRoomAddOptionListDto.Response.builder().roomAddOptionItem(roomAddOptionItemList).build();
    }

    @Transactional
    public BoRoomAddOptionDto.Response modifyRoomAddOptions(BoRoomAddOptionDto.Request request, Long id, MultipartFile file) {
        try {
            RoomExtraOption roomExtraOption = roomExtraOptionService.findRoomAddOptions(id);
            roomExtraOptionService.modifyRoomExtraOption(RoomExtraOption.builder().name(request.getName()).build(), id);

            if(file != null && !file.isEmpty()) {
                File fileEntity = fileService.save(file, FileType.ETC);
                RoomExtraOptionFile roomExtraOptionFile = roomExtraOptionService.findRoomExtraOptionFile(roomExtraOption);
                if(roomExtraOptionFile != null) {
                    roomExtraOptionFile.modifyFile(fileEntity);
                } else {
                    roomExtraOptionService.saveRoomExtraOptionFile(RoomExtraOptionFile.builder()
                            .file(fileEntity)
                            .roomExtraOption(roomExtraOption)
                            .build());
                }
            }
        } catch (IOException e) {
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_FAILED);
        }

        return BoRoomAddOptionDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    @Transactional
    public BoRoomAddOptionDto.Response deleteRoomAddOptions(Long id) {
        roomExtraOptionService.deleteRoomExtraOption(id);
        return BoRoomAddOptionDto.Response.builder().updatedAt(LocalDateTime.now()).build();
    }

    public BoRoomAddOptionDetailDto.Response findRoomAddOptions(Long id) {
        RoomExtraOption roomExtraOption = roomExtraOptionService.findRoomAddOptions(id);
        RoomExtraOptionFile roomExtraOptionFile = roomExtraOptionService.findRoomExtraOptionFile(roomExtraOption);
        String image = (roomExtraOptionFile != null && roomExtraOptionFile.getFile() != null) ? roomExtraOptionFile.getFile().getSavedFileName() : null;
        return BoRoomAddOptionDetailDto.Response.builder().id(roomExtraOption.getId()).name(roomExtraOption.getName()).image(image).build();
    }
}
