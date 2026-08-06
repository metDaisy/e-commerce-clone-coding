package io.github.metdaisy.amaazon.global.exception;

import io.github.metdaisy.amaazon.global.exception.strategy.ExceptionResponseStrategy;
import io.github.metdaisy.amaazon.global.exception.strategy.ExceptionStrategyFactory;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler {

  private final ExceptionStrategyFactory strategyFactory;

  // 클라이언트가 SSE 연결을 끊을 때 발생하는 IOException 무시
  @ExceptionHandler(IOException.class)
  public void handleIOException(IOException e) {
    strategyFactory.getStrategy(IOException.class).buildResponse(e);
  }

  // 존재하지 않는 경로로의 요청(봇/스캐너)에서 발생 시 404 반환
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ExceptionResponse> handleNoResourceFoundException(HttpServletRequest request,
      NoResourceFoundException e) {
    return strategyFactory.buildNoResourceFoundResponse(e, request);
  }

  // 나머지 모든 예외는 전략 팩토리에서 클래스 상속 계층을 기반으로 적절한 전략을 찾아 처리
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ExceptionResponse> handleException(Exception e) {
    ExceptionResponseStrategy<Exception> strategy = strategyFactory.getStrategy(e.getClass());
    return strategy.buildResponse(e);
  }
}
