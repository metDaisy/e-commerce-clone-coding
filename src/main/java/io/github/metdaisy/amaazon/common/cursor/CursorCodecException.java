package io.github.metdaisy.amaazon.common.cursor;

import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;

public class CursorCodecException extends AmaazonException {

  public CursorCodecException(String message, Throwable cause) {
    super(CursorCodecErrorCode.CODEC_FAILURE, AmaazonExceptionContext.systemMessage(message));
    initCause(cause);
  }
}
