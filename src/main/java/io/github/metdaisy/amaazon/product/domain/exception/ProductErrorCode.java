package io.github.metdaisy.amaazon.product.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonErrorCode;
import io.github.metdaisy.amaazon.common.exception.AmaazonErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements AmaazonErrorCode {
  PRODUCT_CATEGORY_NOT_FOUND("PRODUCT-001", "product category 를 찾을 수 없습니다.", AmaazonErrorType.NOT_FOUND),
  MANAGER_NOT_FOUND("PRODUCT-002", "product manager 를 찾을 수 없습니다.", AmaazonErrorType.NOT_FOUND),
  PRODUCT_NOT_FOUND("PRODUCT-003", "product 를 찾을 수 없습니다.", AmaazonErrorType.NOT_FOUND),
  ;

  private final String code;
  private final String message;
  private final AmaazonErrorType errorType;
}
