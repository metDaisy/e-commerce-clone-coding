package io.github.metdaisy.amaazon.catalog.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonErrorCode;
import io.github.metdaisy.amaazon.common.exception.AmaazonErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductVariantErrorCode implements AmaazonErrorCode {
  VARIANT_INVALID("CATALOG-029", "상품 옵션 입력을 확인해 주세요.",
      "ProductVariant 입력 검증 실패", AmaazonErrorType.BAD_REQUEST),
  VARIANT_CREATE_FAILED("CATALOG-030", "요청을 처리하지 못했습니다.",
      "ProductVariant 생성 실패", AmaazonErrorType.INTERNAL_SERVER_ERROR),
  VARIANT_NOT_FOUND("CATALOG-031", "상품 옵션을 찾을 수 없습니다.",
      "ProductVariant 조회 실패", AmaazonErrorType.NOT_FOUND),
  VARIANT_QUERY_FAILED("CATALOG-032", "상품 옵션을 조회하지 못했습니다.",
      "ProductVariant 조회 저장소 실패", AmaazonErrorType.INTERNAL_SERVER_ERROR),
  VARIANT_ARCHIVED("CATALOG-033", "보관된 상품 옵션은 변경할 수 없습니다.",
      "Archived ProductVariant 수정 시도", AmaazonErrorType.CONFLICT),
  VARIANT_UPDATE_FAILED("CATALOG-034", "요청을 처리하지 못했습니다.",
      "ProductVariant 수정 저장소 실패", AmaazonErrorType.INTERNAL_SERVER_ERROR),
  VARIANT_ALREADY_ARCHIVED("CATALOG-035", "이미 보관된 상품 옵션입니다.",
      "ProductVariant 중복 보관 시도", AmaazonErrorType.CONFLICT),
  VARIANT_ARCHIVE_FAILED("CATALOG-036", "요청을 처리하지 못했습니다.",
      "ProductVariant 보관 저장소 실패", AmaazonErrorType.INTERNAL_SERVER_ERROR);

  private final String code;
  private final String message;
  private final String systemMessage;
  private final AmaazonErrorType errorType;
}
