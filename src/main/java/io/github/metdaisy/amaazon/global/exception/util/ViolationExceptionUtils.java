package io.github.metdaisy.amaazon.global.exception.util;

import jakarta.validation.ConstraintViolationException;
import lombok.experimental.UtilityClass;
import org.springframework.web.bind.annotation.RestController;

@UtilityClass
public class ViolationExceptionUtils {

  public boolean isFromController(ConstraintViolationException ex) {
    if (ex == null || ex.getConstraintViolations() == null) {
      return false;
    }
    return ex.getConstraintViolations().stream()
            .anyMatch(v -> v.getRootBeanClass().getSimpleName().endsWith("Controller")
                    || v.getRootBeanClass().isAnnotationPresent(RestController.class));
  }
}
