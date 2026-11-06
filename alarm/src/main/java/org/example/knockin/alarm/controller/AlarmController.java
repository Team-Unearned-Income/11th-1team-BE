package org.example.knockin.meta.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.knockin.alarm.dto.AlarmListDto;
import org.example.knockin.alarm.dto.AlarmReadAllDto;
import org.example.knockin.alarm.dto.AlarmReadDto;
import org.example.knockin.global.api.CommonResponse;
import org.example.knockin.global.auth.dto.PrincipalDetails;
import org.example.knockin.alarm.service.impl.AlarmServiceImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/alarms")
@Tag(name = "9. 알림/고객센터")
public class AlarmController {
    private final AlarmServiceImpl alarmService;

    @GetMapping("")
    @Operation(summary = "알림 목록 조회")
    public CommonResponse<AlarmListDto.Response> findAlarmList(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(alarmService.findAlarmList(pageable, principalDetails.getMember().getId()));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "알림 읽음 처리")
    public CommonResponse<AlarmReadDto.Response> modifyAlarmRead(@PathVariable Long id, @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(alarmService.modifyAlarmRead(id, principalDetails.getMember().getId()));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "알림 전체 읽음 처리")
    public CommonResponse<AlarmReadAllDto.Response> modifyAllAlarmRead(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        return CommonResponse.status(HttpStatus.OK).body(alarmService.modifyAllAlarmRead(principalDetails.getMember().getId()));
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "알림 구독 처리")
    public SseEmitter subscribeAlarm(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        return alarmService.subscribe(principalDetails.getMember().getId());
    }
}

