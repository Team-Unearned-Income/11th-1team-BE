package org.example.knockin.chat.exception;

import org.example.knockin.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChattingErrorCode implements ErrorCode{
    ROOM_NOT_FOUND(9000, HttpStatus.NOT_FOUND, "채팅방 조회에 실패하였습니다."),
    ROOM_MEMBER_NOT_FOUND(9001, HttpStatus.NOT_FOUND, "채팅방 멤버 조회에 실패하였습니다."),
    REQUIRED_NOT_FOUND(9002, HttpStatus.NOT_FOUND, "채팅요청 조회에 실패하였습니다."),
    REQUIRED_DUPLICATE(9003, HttpStatus.BAD_REQUEST, "이미 초대한 채팅요청이 있습니다."),
    ROOM_CAPACITY_EXCEEDED(9004, HttpStatus.BAD_REQUEST, "채팅방 최대 인원이 초과되었습니다."),
    MESSAGE_PAYLOAD_INVALID(9005, HttpStatus.BAD_REQUEST, "채팅 메시지 요청 형식이 올바르지 않습니다."),
    ROOM_ACCESS_DENIED(9006, HttpStatus.FORBIDDEN, "채팅방 접근 권한이 없습니다."),
    ROOM_LIMIT_EXCEEDED(9007, HttpStatus.BAD_REQUEST, "채팅방은 최대 %s개 까지 생성 가능합니다."),
    ROOM_DUPLICATE(9008, HttpStatus.BAD_REQUEST, "이미 활성화된 채팅방이 존재합니다."),
    MESSAGE_BLOCKED(9009, HttpStatus.FORBIDDEN, "차단 관계에서는 채팅 메시지를 전송할 수 없습니다."),
    CANNOT_SEND_TO_LEFT_MEMBER(9010, HttpStatus.BAD_REQUEST, "상대방이 채팅방에 존재하지 않아 채팅을 전송할 수 없습니다."),
    CANNOT_LEAVE_ROOMMATE_CHAT(9011, HttpStatus.FORBIDDEN, "룸메이트로 지정된 채팅방은 나갈수 없습니다."),
    ;

    private final Integer no;
    private final HttpStatus httpStatus;
    private final String message;
}
