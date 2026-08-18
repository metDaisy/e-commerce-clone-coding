package io.github.metdaisy.amaazon.auth.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonErrorCode;
import io.github.metdaisy.amaazon.common.exception.AmaazonErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements AmaazonErrorCode {
  TOKEN_EXPIRED("AUTH-020", "유효하지 않은 Refresh Token입니다.", "Refresh Token 검증 실패",
      AmaazonErrorType.UNAUTHORIZED),
  TOKEN_COMPROMISED("AUTH-021", "이미 사용된 Refresh Token입니다.",
      "회전된 이전 Refresh Token 재사용 감지", AmaazonErrorType.UNAUTHORIZED),
  REFRESH_TOKEN_NOT_FOUND("AUTH-003", "refreshToken DB 에서 해당 토큰을 찾을 수 없습니다.", "Refresh Token 저장 정보 조회 실패", AmaazonErrorType.NOT_FOUND),
  EMAIL_ALREADY_EXISTS("AUTH-004", "이미 가입된 이메일입니다.", "Credential email 중복", AmaazonErrorType.CONFLICT),
  USER_CREDENTIAL_NOT_FOUND("AUTH-005", "유저 정보를 찾을 수 없습니다.", "User credential 조회 실패", AmaazonErrorType.NOT_FOUND),
  UNSUPPORTED_PROVIDER("AUTH-006", "지원하지 않는 로그인 방법입니다.", "지원하지 않는 OAuth provider", AmaazonErrorType.UNSUPPORTED),
  USER_NOT_FOUND("AUTH-007", "유저를 찾을 수 없습니다.", "Auth user 조회 실패", AmaazonErrorType.NOT_FOUND),
  DEVICE_ID_NOT_FOUND("AUTH-008", "deviceId 를 찾을 수 없습니다.", "Device ID 조회 실패", AmaazonErrorType.NOT_FOUND),
  INCORRECT_PASSWORD("AUTH-009", "비밀번호가 일치하지 않습니다.", "비밀번호 검증 실패", AmaazonErrorType.BAD_REQUEST),
  ACCOUNT_DEACTIVATED("AUTH-010", "유저를 찾을 수 없습니다.", "비활성 계정 인증 시도", AmaazonErrorType.UNAUTHORIZED),
  ;

  private final String code;
  private final String message;
  private final String systemMessage;
  private final AmaazonErrorType errorType;
}
