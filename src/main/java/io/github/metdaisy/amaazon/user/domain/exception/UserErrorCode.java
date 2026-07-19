package io.github.metdaisy.amaazon.user.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonErrorCode;
import io.github.metdaisy.amaazon.common.exception.AmaazonErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements AmaazonErrorCode {
  USER_NOT_FOUND("USER-001", "해당 사용자를 찾을 수 없습니다.", AmaazonErrorType.NOT_FOUND),
  NAME_ALREADY_EXISTS("USER-002", "이미 존재하는 이름입니다.", AmaazonErrorType.CONFLICT),
  PHONE_ALREADY_EXISTS("USER-003", "이미 가입된 전화번호입니다.", AmaazonErrorType.CONFLICT);

  private final String code;
  private final String message;
  private final AmaazonErrorType errorType;
}
