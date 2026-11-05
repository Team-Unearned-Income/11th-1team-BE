package org.example.knockin.alarm.exception;

import org.example.knockin.global.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AlarmErrorCode implements ErrorCode {
    ALARM_SEND_ERROR(8000, HttpStatus.INTERNAL_SERVER_ERROR, "알람 전송에 실패하였습니다."),
    ALARM_NOT_FOUND(8001, HttpStatus.INTERNAL_SERVER_ERROR, "알람 조회에 실패하였습니다."),
    PUSH_ALARM_SEND_FAIL(8002, HttpStatus.INTERNAL_SERVER_ERROR, "푸시 알람 전송에 실패하였습니다.");

    private final Integer no;
    private final HttpStatus httpStatus;
    private final String message;
}
