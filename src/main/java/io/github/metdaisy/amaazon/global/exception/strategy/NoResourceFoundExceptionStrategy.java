package io.github.metdaisy.amaazon.global.exception.strategy;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import io.github.metdaisy.amaazon.global.exception.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * NoResourceFoundException 처리 전략
 */
@Slf4j
public class NoResourceFoundExceptionStrategy extends AbstractExceptionResponseStrategy<NoResourceFoundException> {

  @Override
  protected boolean logExceptionMessage() {
    return true;
  }

  @Override
  protected void logException(NoResourceFoundException exception) {
    log.warn("존재하지 않는 리소스 요청: {}", exception.getMessage());
  }

  @Override
  protected ApiErrorResponse createErrorResponse(NoResourceFoundException exception) {
    return new ApiErrorResponse("NOT_FOUND", "요청하신 리소스를 찾을 수 없습니다.", null);
  }

  @Override
  protected HttpStatus getHttpStatus(NoResourceFoundException exception) {
    return HttpStatus.NOT_FOUND;
  }

  /**
   * HttpServletRequest를 사용하여 응답을 생성합니다. User-Agent 헤더를 로그에 포함합니다.
   */
  protected ResponseEntity<ApiErrorResponse> buildResponse(NoResourceFoundException exception,
      HttpServletRequest request) {
    String userAgent = request.getHeader("User-Agent");
    log.warn("존재하지 않는 리소스 요청: {} | User-Agent: {}",
        exception.getMessage(), userAgent);
    return ResponseEntity
        .status(getHttpStatus(exception))
        .body(createErrorResponse(exception));
  }
}
