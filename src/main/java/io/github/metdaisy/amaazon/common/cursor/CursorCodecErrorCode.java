package io.github.metdaisy.amaazon.common.cursor;

import io.github.metdaisy.amaazon.common.exception.AmaazonErrorCode;
import io.github.metdaisy.amaazon.common.exception.AmaazonErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
enum CursorCodecErrorCode implements AmaazonErrorCode {
  CODEC_FAILURE("CURSOR-001", "커서 처리 중 오류가 발생했습니다.", "Cursor codec 처리 실패",
      AmaazonErrorType.INTERNAL_SERVER_ERROR),
  ;

  private final String code;
  private final String message;
  private final String systemMessage;
  private final AmaazonErrorType errorType;
}
