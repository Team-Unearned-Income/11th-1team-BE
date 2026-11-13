package org.example.knockin.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LifePatternErrorCode implements ErrorCode {
    LIFE_PATTERN_NOT_FOUNT(16000, HttpStatus.INTERNAL_SERVER_ERROR, "생활 패턴을 조회하지 못하였습니다."),
    DEGREE_NOT_FOUND(16001, HttpStatus.NOT_FOUND, "생활 패턴 차수를 조회하지 못하였습니다."),
    LIFE_PATTERN_FILE_NOT_FOUND(16002, HttpStatus.NOT_FOUND, "생활 패턴 파일을 조회하지 못하였습니다."),
    ;

    private final Integer no;
    private final HttpStatus httpStatus;
    private final String message;
}
