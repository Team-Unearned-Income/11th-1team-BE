package org.example.knockin.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FileErrorCode implements ErrorCode {
    FILE_EMPTY(6000, HttpStatus.BAD_REQUEST, "파일이 비어 있습니다."),
    FILE_EXTENSION_MISSING(6001, HttpStatus.BAD_REQUEST, "파일 확장자가 없습니다."),
    FILE_UNSUPPORTED_TYPE(6002, HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."),
    FILE_UPLOAD_FAILED(6003, HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    FILE_DELETE_FAILED(6004, HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제에 실패했습니다."),
    FILE_NOT_FOUND(6005, HttpStatus.NOT_FOUND, "파일 조회에 실패했습니다."),
    FILE_UPLOAD_INVALID(6006, HttpStatus.BAD_REQUEST, "업로드 파일 정보가 유효하지 않습니다."),
    FILE_UPLOAD_UNAVAILABLE(6007, HttpStatus.SERVICE_UNAVAILABLE, "현재 환경에서는 R2 직접 업로드를 사용할 수 없습니다."),
    ;

    private final Integer no;
    private final HttpStatus httpStatus;
    private final String message;
}
