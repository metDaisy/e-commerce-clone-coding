package io.github.metdaisy.amaazon.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import io.github.metdaisy.amaazon.global.exception.util.ViolationExceptionUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExceptionResponse(
    int statusCode,
    String exceptionCode,
    String message,
    Object details,
    Instant timestamp
) {

  public static ExceptionResponse from(AmaazonException ex, HttpStatus status) {
    return of(status, ex.getCode(), ex.getClientMessage(), detailsOrNull(ex.getClientDetails()));
  }

  public static ExceptionResponse from(MethodArgumentNotValidException ex, HttpStatus status) {
    Map<String, List<String>> details = ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.groupingBy(
            FieldError::getField,
            Collectors.mapping(
                error -> error.getDefaultMessage() == null ? "유효하지 않은 입력값입니다."
                    : error.getDefaultMessage(),
                Collectors.toList()
            )
        ));
    return of(status, "INVALID_INPUT", "잘못된 입력값입니다.", details);
  }

  public static ExceptionResponse from(ConstraintViolationException ex, HttpStatus status) {
    boolean isFromController = ViolationExceptionUtils.isFromController(ex);
    if (isFromController) {
      Map<String, List<String>> details = ex.getConstraintViolations().stream()
          .collect(Collectors.groupingBy(
              v -> {
                String path = v.getPropertyPath().toString();
                return path.substring(path.lastIndexOf('.') + 1);
              },
              Collectors.mapping(ConstraintViolation::getMessage, Collectors.toList())
          ));
      return of(status, "INVALID_INPUT", "잘못된 입력값입니다.", details);
    }
    return of(status, "SYSTEM-001", "요청을 처리하지 못했습니다.", null);
  }

  public static ExceptionResponse from(HttpMessageNotReadableException ex, HttpStatus status) {
    if (ex.getCause() instanceof InvalidFormatException invalidFormatException) {
      List<JsonMappingException.Reference> path = invalidFormatException.getPath();

      String fieldName = "request";

      // 배열 인덱스 등 fieldName이 null인 경우를 고려해 뒤에서부터 실제 필드명을 찾는다.
      for (int i = path.size() - 1; i >= 0; i--) {
        String candidate = path.get(i).getFieldName();
        if (candidate != null) {
          fieldName = candidate;
          break;
        }
      }

      Object invalidValue = invalidFormatException.getValue();

      return of(status, "INVALID_INPUT", "잘못된 입력값입니다.",
          Map.of(fieldName, List.of("허용되지 않는 값입니다: " + invalidValue)));
    }

    return of(status, "INVALID_INPUT", "요청 본문을 읽을 수 없습니다.", null);
  }

  public static ExceptionResponse from(
      MissingServletRequestParameterException ex,
      HttpStatus status
  ) {
    Map<String, List<String>> details = Map.of(
        ex.getParameterName(),
        List.of("필수 파라미터 '" + ex.getParameterName() + "'이(가) 누락되었습니다.")
    );
    return of(status, "INVALID_INPUT", "잘못된 입력값입니다.", details);
  }

  public static ExceptionResponse from(
      MethodArgumentTypeMismatchException ex,
      HttpStatus status
  ) {
    Map<String, List<String>> details = Map.of(
        ex.getName(),
        List.of("허용되지 않는 값입니다: " + ex.getValue())
    );
    return of(status, "INVALID_INPUT", "잘못된 입력값입니다.", details);
  }

  public static ExceptionResponse from(DataIntegrityViolationException ex, HttpStatus status) {
    return of(status, "CONFLICT", "이미 존재하거나 사용할 수 없는 데이터입니다.", null);
  }

  public static ExceptionResponse from(DisabledException ex, HttpStatus status) {
    return of(status, "USER-004", "비활성화된 계정입니다.", null);
  }

  public static ExceptionResponse from(AuthenticationException ex, HttpStatus status) {
    return of(status, "AUTH-001", "인증 정보가 유효하지 않습니다.", null);
  }

  public static ExceptionResponse from(AccessDeniedException ex, HttpStatus status) {
    return of(status, "ADMIN-001", "권한이 없습니다.", null);
  }

  public static ExceptionResponse of(
      HttpStatus status,
      String exceptionCode,
      String message,
      Object details
  ) {
    return new ExceptionResponse(status.value(), exceptionCode, message, details, Instant.now());
  }

  private static Object detailsOrNull(Map<String, Object> details) {
    return details.isEmpty() ? null : details;
  }
}
