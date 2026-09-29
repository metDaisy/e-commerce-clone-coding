package io.github.metdaisy.amaazon.cart.domain.exception;

import io.github.metdaisy.amaazon.common.exception.AmaazonErrorCode;
import io.github.metdaisy.amaazon.common.exception.AmaazonErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CartErrorCode implements AmaazonErrorCode {
  CART_NOT_FOUND("CART-001", "장바구니 항목을 찾을 수 없습니다.", "Cart 또는 Cart Item 조회 실패", AmaazonErrorType.NOT_FOUND),
  CART_INVALID_INPUT("CART-002", "상품과 수량을 확인해 주세요.", "Cart 입력 검증 실패", AmaazonErrorType.BAD_REQUEST),
  CART_OFFER_LIMIT("CART-003", "구매 가능한 수량을 초과했습니다.", "Offer 구매 제한 초과", AmaazonErrorType.BAD_REQUEST),
  CART_ITEM_LIMIT("CART-004", "장바구니 상품 종류가 너무 많습니다.", "Cart 상품 종류 제한 초과", AmaazonErrorType.BAD_REQUEST),
  CART_QUANTITY_LIMIT("CART-005", "장바구니 수량이 너무 많습니다.", "Cart 전체 수량 제한 초과", AmaazonErrorType.BAD_REQUEST),
  CART_OFFER_UNAVAILABLE("CART-006", "현재 구매할 수 없는 상품입니다.", "Offer가 판매 중지 또는 보관됨", AmaazonErrorType.CONFLICT),
  CART_MERGE_CONFLICT("CART-007", "장바구니를 병합할 수 없습니다.", "Cart 병합 제한 초과", AmaazonErrorType.CONFLICT);

  private final String code;
  private final String message;
  private final String systemMessage;
  private final AmaazonErrorType errorType;
}
