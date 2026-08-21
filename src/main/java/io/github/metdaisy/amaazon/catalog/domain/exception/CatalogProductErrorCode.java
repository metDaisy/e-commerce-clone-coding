package io.github.metdaisy.amaazon.catalog.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonErrorCode;
import io.github.metdaisy.amaazon.common.exception.AmaazonErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CatalogProductErrorCode implements AmaazonErrorCode {
  MANAGER_NOT_FOUND("CATALOG-002", "product manager 를 찾을 수 없습니다.", "Product manager 조회 실패", AmaazonErrorType.NOT_FOUND),
  CATALOG_NOT_FOUND("CATALOG-003", "catalog product 를 찾을 수 없습니다.", "CatalogProduct 조회 실패", AmaazonErrorType.NOT_FOUND),
  UNAUTHORIZED_UPDATE("CATALOG-004", "권한이 없습니다.", "CatalogProduct 수정 권한 없음", AmaazonErrorType.UNAUTHORIZED),
  SELLER_APPROVAL_REQUIRED("CATALOG-005", "판매자만 가능합니다.", "Seller 승인 권한 필요", AmaazonErrorType.UNAUTHORIZED),
  PRODUCT_CODE_ERROR("CATALOG-006", "상품 식별자가 옳바르지 않거나 이미 등록되어 있습니다.", "상품 식별자 검증 또는 중복 실패", AmaazonErrorType.BAD_REQUEST),
  CATALOG_PRODUCT_ARCHIVED("CATALOG-007", "아카이브된 상품 카탈로그입니다.", "Archived CatalogProduct 접근", AmaazonErrorType.CONFLICT),
  ;

  private final String code;
  private final String message;
  private final String systemMessage;
  private final AmaazonErrorType errorType;
}
