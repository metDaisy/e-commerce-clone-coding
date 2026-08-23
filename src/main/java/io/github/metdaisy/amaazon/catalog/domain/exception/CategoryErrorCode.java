package io.github.metdaisy.amaazon.catalog.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonErrorCode;
import io.github.metdaisy.amaazon.common.exception.AmaazonErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CategoryErrorCode implements AmaazonErrorCode {
  CATEGORY_NAME_INVALID("CATEGORY-001", "Category name must not be blank.",
      "Category name 검증 실패", AmaazonErrorType.BAD_REQUEST),
  CATEGORY_DEPTH_EXCEEDED("CATEGORY-002", "Category depth must not exceed three levels.",
      "Category depth 제한 초과", AmaazonErrorType.BAD_REQUEST),
  CATEGORY_NOT_FOUND("CATEGORY-003", "카테고리를 찾을 수 없습니다.", "Category 조회 실패",
      AmaazonErrorType.NOT_FOUND),
  CATEGORY_NAME_DUPLICATE("CATEGORY-004", "Category name already exists.",
      "Category 이름 전역 중복", AmaazonErrorType.CONFLICT),
  CATEGORY_CYCLE_DETECTED("CATEGORY-005", "Category hierarchy cannot contain a cycle.",
      "Category hierarchy 순환 감지", AmaazonErrorType.BAD_REQUEST);

  private final String code;
  private final String message;
  private final String systemMessage;
  private final AmaazonErrorType errorType;
}
