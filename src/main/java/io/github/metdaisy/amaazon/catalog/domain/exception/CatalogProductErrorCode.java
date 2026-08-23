package io.github.metdaisy.amaazon.catalog.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonErrorCode;
import io.github.metdaisy.amaazon.common.exception.AmaazonErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CatalogProductErrorCode implements AmaazonErrorCode {
  CATALOG_NOT_FOUND("CATALOG-019", "catalog product 를 찾을 수 없습니다.", "CatalogProduct 조회 실패", AmaazonErrorType.NOT_FOUND),
  PRODUCT_CODE_ERROR("CATALOG-006", "상품 식별자가 옳바르지 않거나 이미 등록되어 있습니다.", "상품 식별자 검증 또는 중복 실패", AmaazonErrorType.BAD_REQUEST),
  CATALOG_PRODUCT_ARCHIVED("CATALOG-007", "아카이브된 상품 카탈로그입니다.", "Archived CatalogProduct 접근", AmaazonErrorType.CONFLICT),
  CATALOG_PRODUCT_INVALID("CATALOG-013", "상품 정보를 확인해 주세요.", "CatalogProduct 생성 입력 검증 실패", AmaazonErrorType.BAD_REQUEST),
  IDENTIFIER_INVALID("CATALOG-014", "상품 식별자 입력을 확인해 주세요.", "CatalogProduct 식별자 형식 검증 실패", AmaazonErrorType.BAD_REQUEST),
  ISBN_EXTERNAL_VERIFICATION_FAILED("CATALOG-015", "상품 식별자 입력을 확인해 주세요.", "ISBN 외부 검증 실패", AmaazonErrorType.BAD_REQUEST),
  IDENTIFIER_DUPLICATE("CATALOG-017", "이미 등록된 상품 식별자입니다.", "CatalogProduct 식별자 중복", AmaazonErrorType.CONFLICT),
  CATALOG_PRODUCT_CREATE_FAILED("CATALOG-018", "요청을 처리하지 못했습니다.", "CatalogProduct 생성 실패", AmaazonErrorType.INTERNAL_SERVER_ERROR),
  ;

  private final String code;
  private final String message;
  private final String systemMessage;
  private final AmaazonErrorType errorType;
}
