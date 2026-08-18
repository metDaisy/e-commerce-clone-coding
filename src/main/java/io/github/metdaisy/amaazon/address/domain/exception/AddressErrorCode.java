package io.github.metdaisy.amaazon.address.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonErrorCode;
import io.github.metdaisy.amaazon.common.exception.AmaazonErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AddressErrorCode implements AmaazonErrorCode {
  ADDRESS_NOT_FOUND("ADDRESS-004", "주소를 찾을 수 없습니다.", "Address 조회 실패", AmaazonErrorType.NOT_FOUND),
  INVALID_ADDRESS("ADDRESS-005", "주소 정보를 확인해 주세요.", "Address 입력값 검증 실패",
      AmaazonErrorType.BAD_REQUEST),
  ADDRESS_LIMIT_EXCEEDED("ADDRESS-006", "등록할 수 있는 주소 수를 초과했습니다.",
      "Address 최대 개수 초과", AmaazonErrorType.BAD_REQUEST),
  ADDRESS_ACCESS_DENIED("ADDRESS-008", "주소를 변경할 권한이 없습니다.",
      "Address 소유권 검증 실패", AmaazonErrorType.FORBIDDEN);

  private final String code;
  private final String message;
  private final String systemMessage;
  private final AmaazonErrorType errorType;
}
