package io.github.metdaisy.amaazon.catalog.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonErrorCode;
import io.github.metdaisy.amaazon.common.exception.AmaazonErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CategoryErrorCode implements AmaazonErrorCode {
  CATEGORY_NOT_FOUND("CATEGORY-001", "product category 를 찾을 수 없습니다.", "Category 조회 실패",
      AmaazonErrorType.NOT_FOUND),
  CATEGORY_DEPTH_EXCEEDED("CATEGORY-008", "Category depth must not exceed three levels.",
      "Category depth 제한 초과", AmaazonErrorType.BAD_REQUEST),
  CATEGORY_CYCLE_DETECTED("CATEGORY-009", "Category hierarchy cannot contain a cycle.",
      "Category hierarchy 순환 감지", AmaazonErrorType.BAD_REQUEST),
  CATEGORY_UPDATE_EMPTY("CATEGORY-010", "At least one category field must be provided.",
      "Category 수정 필드 없음", AmaazonErrorType.BAD_REQUEST),
  CATEGORY_NAME_INVALID("CATEGORY-011", "Category name must not be blank.",
      "Category name 검증 실패", AmaazonErrorType.BAD_REQUEST),
  CATEGORY_NAME_DUPLICATE("CATEGORY-012", "Category name already exists.",
      "Category 이름 전역 중복", AmaazonErrorType.CONFLICT),
  ;

  private final String code;
  private final String message;
  private final String systemMessage;
  private final AmaazonErrorType errorType;
}
