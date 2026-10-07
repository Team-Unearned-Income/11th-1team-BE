package org.example.knockin.board.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.knockin.board.dto.BoardDetailDto;
import org.example.knockin.board.dto.BoardDto;
import org.example.knockin.board.dto.BoardDto.Response;
import org.example.knockin.board.dto.BoardEditDto;
import org.example.knockin.board.dto.BoardListDto;
import org.example.knockin.board.dto.BoardModifyDto;
import org.example.knockin.util.dto.MatchDetailDto;
import org.example.knockin.util.dto.MatchDto;
import org.example.knockin.util.dto.MatchListDto;
import org.example.knockin.util.dto.MatchScoreDto;
import org.example.knockin.declaration.dto.MemberReportDto;
import org.example.knockin.declaration.dto.ReportDto;
import org.example.knockin.global.api.CommonResponse;
import org.example.knockin.global.auth.dto.PrincipalDetails;
import org.example.knockin.board.service.RoommateBoardService;
import org.example.knockin.mate.service.RoommateMatchingService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/roommate")
@Tag(name = "4. 룸메이트 게시물/매칭")
public class RoomMateController {

    public final RoommateBoardService roommateBoardService;
    private final RoommateMatchingService roommateMatchingService;

    @GetMapping("/boards")
    @Operation(
            summary = "게시글 목록 조회",
            description = "likedOnly=true는 로그인 사용자만 사용할 수 있습니다."
    )
    public CommonResponse<Slice<BoardListDto.Response>> findBoardList(
            @AuthenticationPrincipal PrincipalDetails details,
            @ParameterObject @Validated @ModelAttribute BoardListDto.Request request,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Long memberId = details == null || details.getMember() == null ? null : details.getMember().getId();
        Slice<BoardListDto.Response> responses = roommateBoardService.getBoardList(request, pageable, memberId);
        return CommonResponse.status(HttpStatus.OK).body(responses);
    }

    @GetMapping("/boards/{boardId}")
    @Operation(summary = "게시글 상세 조회")
    public CommonResponse<BoardDetailDto.Response> findBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal PrincipalDetails details
    ) {
        Long memberId = details.getMember().getId();
        return CommonResponse.status(HttpStatus.OK).body(roommateBoardService.getBoardDetail(boardId, memberId));
    }

    @PostMapping("/boards/{boardId}/likes")
    @Operation(summary = "게시글 찜하기")
    public CommonResponse<BoardDto.Response> likeBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal PrincipalDetails details
    ) {
        Long memberId = details.getMember().getId();
        Response response = roommateBoardService.likeBoard(boardId, memberId);
        return CommonResponse.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/boards/{boardId}/edit")
    @Operation(summary = "게시글 편집 form")
    public CommonResponse<BoardEditDto.Response> findEditForm(
            @AuthenticationPrincipal PrincipalDetails principalDetails, @PathVariable Long boardId) {
        Long memberId = principalDetails.getMember().getId();
        BoardEditDto.Response response = roommateBoardService.getEditForm(memberId, boardId);
        return CommonResponse.status(HttpStatus.OK).body(response);
    }

    @PostMapping(value = "/boards", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "게시글 저장")
    public CommonResponse<BoardDto.Response> saveBoard(
            @Valid @RequestPart("request") BoardDto.Request request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal PrincipalDetails details) {
        Long memberId = details.getMember().getId();
        Response response = roommateBoardService.save(request, memberId, files);
        return CommonResponse.status(HttpStatus.OK).body(response);
    }

    @PutMapping(value = "/boards/{boardId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "게시글 수정")
    public CommonResponse<BoardModifyDto.Response> modifyBoard(
            @PathVariable Long boardId,
            @Valid @RequestPart("request") BoardModifyDto.Request request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal PrincipalDetails details
    ) {
        Long memberId = details.getMember().getId();
        BoardModifyDto.Response response = roommateBoardService.modify(memberId, boardId, request, files);
        return CommonResponse.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/boards/{boardId}/reports")
    @Operation(summary = "게시글 신고")
    public CommonResponse<ReportDto.Response> reportBoard(
            @PathVariable Long boardId,
            @Valid @RequestBody ReportDto.Request request,
            @AuthenticationPrincipal PrincipalDetails details
    ) {
        Long memberId = details.getMember().getId();
        ReportDto.Response response = roommateBoardService.reportBoard(request, boardId, memberId);
        return CommonResponse.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/boards/{boardId}")
    @Operation(summary = "게시글 삭제")
    public CommonResponse<BoardDto.Response> deleteBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal PrincipalDetails details
    ) {
        Long memberId = details.getMember().getId();
        Response response = roommateBoardService.deleteBoard(boardId, memberId);
        return CommonResponse.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/matches")
    @Operation(
            summary = "매칭 목록 조회",
            description = "likedOnly=true는 로그인 사용자만 사용할 수 있습니다."
    )
    public CommonResponse<Slice<MatchListDto.Response>> findMatchList(
            @AuthenticationPrincipal PrincipalDetails details,
            @ParameterObject @Validated @ModelAttribute MatchListDto.Request request
    ) {
        Long memberId = details == null ? null : details.getMember().getId();
        Slice<MatchListDto.Response> responses = roommateMatchingService.findMatchingList(memberId, request);
        return CommonResponse.status(HttpStatus.OK).body(responses);
    }

    @GetMapping("/matches/{memberId}")
    @Operation(summary = "매칭 상세 조회")
    public CommonResponse<MatchDetailDto.Response> findMatch(
            @AuthenticationPrincipal PrincipalDetails details,
            @PathVariable Long memberId
    ) {
        Long requesterId = details == null ? null : details.getMember().getId();
        MatchDetailDto.Response response = roommateMatchingService.findMatchingDetail(memberId, requesterId);
        return CommonResponse.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/matches/score")
    @Operation(summary = "매칭 점수 조회")
    public CommonResponse<MatchScoreDto.Response> findMatchScore() {
        return CommonResponse.status(HttpStatus.OK).body(new MatchScoreDto.Response());
    }

    @PostMapping("/matches/{memberId}/likes")
    @Operation(summary = "매칭 사용자 찜하기")
    public CommonResponse<MatchDto.Response> likeBoard(
            @AuthenticationPrincipal PrincipalDetails details,
            @PathVariable Long memberId
    ) {
        Long senderId = details.getMember().getId();
        MatchDto.Response response = roommateMatchingService.likeMatching(senderId, memberId);
        return CommonResponse.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/matches/{memberId}/reports")
    @Operation(summary = "매칭 사용자 신고")
    public CommonResponse<MemberReportDto.Response> reportBoard(
            @PathVariable Long memberId,
            @Valid @RequestBody MemberReportDto.Request request,
            @AuthenticationPrincipal PrincipalDetails details
    ) {
        Long reporterId = details.getMember().getId();
        MemberReportDto.Response response = roommateMatchingService.reportMatching(reporterId, memberId, request);
        return CommonResponse.status(HttpStatus.OK).body(response);
    }
}

