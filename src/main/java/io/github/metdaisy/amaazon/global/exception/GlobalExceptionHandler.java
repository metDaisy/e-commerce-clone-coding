package io.github.metdaisy.amaazon.global.exception;

import java.io.IOException;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import io.github.metdaisy.amaazon.global.exception.strategy.ExceptionStrategyFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final HttpServletRequest request;
  private final ExceptionStrategyFactory strategyFactory;

  @ExceptionHandler(AmaazonException.class)
  public ResponseEntity<ApiErrorResponse> handleBusinessException(AmaazonException e) {
    return strategyFactory.getStrategy(AmaazonException.class).buildResponse(e);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException e) {
    return strategyFactory.getStrategy(MethodArgumentNotValidException.class).buildResponse(e);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
      ConstraintViolationException e) {
    return strategyFactory.getStrategy(ConstraintViolationException.class).buildResponse(e);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException e) {
    return strategyFactory.getStrategy(HttpMessageNotReadableException.class).buildResponse(e);
  }

  // 클라이언트가 SSE 연결을 끊을 때(브라우저 탭 닫기, 새로고침 등) 응답 스트림에 쓰다가 발생하는
  // broken pipe성 IOException. 클라이언트가 이미 떠난 상태라 응답을 보낼 수 없고, 버그도 아니므로
  // ERROR로 로그를 남기지 않고 조용히 무시한다.
  @ExceptionHandler(IOException.class)
  public void handleIOException(IOException e) {
    strategyFactory.getStrategy(IOException.class).buildResponse(e);
  }

  // 존재하지 않는 경로로의 요청(봇/스캐너의 무작위 경로 탐색)에서 발생
  // 버그가 아니므로 ERROR로 로그를 남기지 않고 404만 반환
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNoResourceFoundException(
      NoResourceFoundException e) {
    return strategyFactory.buildNoResourceFoundResponse(e, request);
  }

  // 배치 작업 스레드 풀의 큐까지 가득 찬 상태에서 새 수집 요청이 들어오면 AbortPolicy가
  // TaskRejectedException을 던진다. 일시적인 과부하이므로 503으로 매핑해 재시도를 유도한다.
  @ExceptionHandler(TaskRejectedException.class)
  public ResponseEntity<ApiErrorResponse> handleTaskRejectedException(TaskRejectedException e) {
    return strategyFactory.getStrategy(TaskRejectedException.class).buildResponse(e);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleException(Exception e) {
    return strategyFactory.getStrategy(Exception.class).buildResponse(e);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiErrorResponse> handleMissingServletRequestParameterException(
      MissingServletRequestParameterException e) {
    return strategyFactory.getStrategy(MissingServletRequestParameterException.class).buildResponse(e);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatchException(
      MethodArgumentTypeMismatchException e) {
    return strategyFactory.getStrategy(MethodArgumentTypeMismatchException.class).buildResponse(e);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolationException(
      DataIntegrityViolationException e) {
    return strategyFactory.getStrategy(DataIntegrityViolationException.class).buildResponse(e);
  }
}
