package io.github.metdaisy.amaazon.address.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonErrorCode;
import io.github.metdaisy.amaazon.common.exception.AmaazonErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AddressErrorCode implements AmaazonErrorCode {
  ADDRESS_NOT_FOUND("ADDRESS-004", "주소를 찾을 수 없습니다.", "Address 조회 실패", AmaazonErrorType.NOT_FOUND),
  ADDRESS_DUPLICATED("ADDRESS-005", "이미 등록된 주소입니다.", "Address 중복 등록", AmaazonErrorType.BAD_REQUEST),
  ADDRESS_ACCESS_DENIED("ADDRESS-008", "주소를 변경할 권한이 없습니다.",
      "Address 소유권 검증 실패", AmaazonErrorType.FORBIDDEN);

  private final String code;
  private final String message;
  private final String systemMessage;
  private final AmaazonErrorType errorType;
}
