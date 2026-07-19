package io.github.metdaisy.amaazon.global.security.jwt.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonErrorCode;
import io.github.metdaisy.amaazon.common.exception.AmaazonErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JwtErrorCode implements AmaazonErrorCode {
  TOKEN_EXPIRED("JWT-001", "만료된 JWT 토큰입니다.", AmaazonErrorType.UNAUTHORIZED),
  TOKEN_PARSE_FAILED("JWT-002", "토큰 파싱에 실패했습니다.", AmaazonErrorType.BAD_REQUEST),
  TOKEN_NOT_FOUND("JWT-003", "cache 에 저장된 토큰을 찾을 수 없습니다.", AmaazonErrorType.NOT_FOUND),
  SIGN_FAILED("JWT-004", "토큰 서명에 실패했습니다.", AmaazonErrorType.INTERNAL_SERVER_ERROR),
  VERIFICATION_FAILED("JWT-005", "토큰 서명 검증 과정에서 오류가 발생했습니다.", AmaazonErrorType.INTERNAL_SERVER_ERROR),
  INVALID_SIGNATURE("JWT-006", "유효하지 않은 토큰 서명입니다.", AmaazonErrorType.UNAUTHORIZED),
  ;
  private final String code;
  private final String message;
  private final AmaazonErrorType errorType;
}
