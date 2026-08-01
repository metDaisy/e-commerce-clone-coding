package io.github.metdaisy.amaazon.auth.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonErrorCode;
import io.github.metdaisy.amaazon.common.exception.AmaazonErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements AmaazonErrorCode {
  TOKEN_EXPIRED("AUTH-001", "만료된 토큰입니다.", AmaazonErrorType.BAD_REQUEST),
  TOKEN_COMPROMISED("AUTH-002", "위변조된 토큰입니다.", AmaazonErrorType.BAD_REQUEST),
  REFRESH_TOKEN_NOT_FOUND("AUTH-003", "refreshToken DB 에서 해당 토큰을 찾을 수 없습니다.", AmaazonErrorType.NOT_FOUND),
  EMAIL_ALREADY_EXISTS("AUTH-004", "이미 가입된 이메일입니다.", AmaazonErrorType.CONFLICT),
  USER_CREDENTIAL_NOT_FOUND("AUTH-005", "유저 정보를 찾을 수 없습니다.", AmaazonErrorType.NOT_FOUND),
  UNSUPPORTED_PROVIDER("AUTH-006", "지원하지 않는 로그인 방법입니다.", AmaazonErrorType.UNSUPPORTED),
  USER_NOT_FOUND("AUTH-007", "유저를 찾을 수 없습니다.", AmaazonErrorType.NOT_FOUND),
  DEVICE_ID_NOT_FOUND("AUTH-008", "deviceId 를 찾을 수 없습니다.", AmaazonErrorType.NOT_FOUND),
  INCORRECT_PASSWORD("AUTH-009", "비밀번호가 일치하지 않습니다.", AmaazonErrorType.BAD_REQUEST),
  ;

  private final String code;
  private final String message;
  private final AmaazonErrorType errorType;
}
