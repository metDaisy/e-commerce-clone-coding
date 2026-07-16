package io.github.metdaisy.amaazon.global.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import io.github.metdaisy.amaazon.global.exception.util.ViolationExceptionUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

public record ApiErrorResponse(String exceptionType, String message, Object details) {

  public static ApiErrorResponse from(AmaazonException ex) {
    return new ApiErrorResponse(ex.getExceptionType(), ex.getMessage(), null);
  }

  public static ApiErrorResponse from(MethodArgumentNotValidException ex) {
    Map<String, List<String>> details = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.groupingBy(
                    FieldError::getField,
                    Collectors.mapping(
                            error -> error.getDefaultMessage() == null ? "유효하지 않은 입력값입니다."
                                    : error.getDefaultMessage(),
                            Collectors.toList()
                    )
            ));
    return new ApiErrorResponse("INVALID_INPUT", "잘못된 입력값입니다.", details);
  }

  public static ApiErrorResponse from(ConstraintViolationException ex) {
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
      return new ApiErrorResponse("INVALID_INPUT", "잘못된 입력값입니다.", details);
    }
    return new ApiErrorResponse("INTERNAL_SERVER_ERROR",
            "서버 내부 데이터 처리 중 오류가 발생했습니다.", null);
  }

  public static ApiErrorResponse from(HttpMessageNotReadableException ex) {
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

      return new ApiErrorResponse(
              "INVALID_INPUT",
              "잘못된 입력값입니다.",
              Map.of(
                      fieldName,
                      List.of("허용되지 않는 값입니다: " + invalidValue)
              )
      );
    }

    return new ApiErrorResponse(
            "INVALID_INPUT",
            "요청 본문을 읽을 수 없습니다.",
            null
    );
  }

  public static ApiErrorResponse from(MissingServletRequestParameterException ex) {
    Map<String, List<String>> details = Map.of(
            ex.getParameterName(),
            List.of("필수 파라미터 '" + ex.getParameterName() + "'이(가) 누락되었습니다.")
    );
    return new ApiErrorResponse("INVALID_INPUT", "잘못된 입력값입니다.", details);
  }

  public static ApiErrorResponse from(MethodArgumentTypeMismatchException ex) {
    Map<String, List<String>> details = Map.of(
            ex.getName(),
            List.of("허용되지 않는 값입니다: " + ex.getValue())
    );
    return new ApiErrorResponse("INVALID_INPUT", "잘못된 입력값입니다.", details);
  }

  public static ApiErrorResponse from(DataIntegrityViolationException ex) {
    return new ApiErrorResponse(
            "CONFLICT",
            "이미 존재하거나 사용할 수 없는 데이터입니다.",
            null
    );
  }
}
