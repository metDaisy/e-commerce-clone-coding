package io.github.metdaisy.amaazon.cart.application.port.out;

import io.github.metdaisy.amaazon.common.exception.AmaazonErrorCode;
import io.github.metdaisy.amaazon.common.exception.AmaazonErrorType;
import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class OfferNotFoundException extends AmaazonException {

  public OfferNotFoundException(UUID offerId) {
    super(ErrorCode.OFFER_NOT_FOUND,
        AmaazonExceptionContext.logDetails(Map.of("offerId", offerId)));
  }

  @Getter
  @RequiredArgsConstructor
  private enum ErrorCode implements AmaazonErrorCode {
    OFFER_NOT_FOUND("OFFER-001", "판매 조건을 찾을 수 없습니다.", "Offer 식별자 조회 실패",
        AmaazonErrorType.NOT_FOUND);

    private final String code;
    private final String message;
    private final String systemMessage;
    private final AmaazonErrorType errorType;
  }
}
