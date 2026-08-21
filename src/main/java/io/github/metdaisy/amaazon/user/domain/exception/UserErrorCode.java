package io.github.metdaisy.amaazon.user.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonErrorCode;
import io.github.metdaisy.amaazon.common.exception.AmaazonErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements AmaazonErrorCode {
  USER_NOT_FOUND("USER-001", "해당 사용자를 찾을 수 없습니다.", "User 조회 실패", AmaazonErrorType.NOT_FOUND),
  NAME_ALREADY_EXISTS("USER-002", "이미 존재하는 이름입니다.", "User name 중복", AmaazonErrorType.CONFLICT),
  PHONE_ALREADY_EXISTS("USER-003", "이미 가입된 전화번호입니다.", "User phone 중복", AmaazonErrorType.CONFLICT),
  USER_ALREADY_DISABLED("USER-010", "이미 비활성화된 계정입니다.", "User already disabled", AmaazonErrorType.CONFLICT),
  USER_DISABLED("USER-004", "비활성화된 계정입니다.", "비활성 User 접근", AmaazonErrorType.FORBIDDEN);

  private final String code;
  private final String message;
  private final String systemMessage;
  private final AmaazonErrorType errorType;
}
