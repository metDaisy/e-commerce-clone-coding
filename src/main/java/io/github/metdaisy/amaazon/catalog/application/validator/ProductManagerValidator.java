package io.github.metdaisy.amaazon.catalog.application.validator;

import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class ProductManagerValidator {

  private final CatalogProductRepository repository;

  @Before("@annotation(isManager)")
  public void validate(JoinPoint point, IsManager isManager) {
    AmaazonPrincipal principal = (AmaazonPrincipal) SecurityContextHolder.getContext()
        .getAuthentication().getPrincipal();
    UUID managerId = principal.getId();
    UUID productId = extractProductId(point, isManager.productId());
    if (!repository.existByIdAndManagerId(productId, managerId)) {
      throw new CatalogProductException(CatalogProductErrorCode.UNAUTHORIZED_UPDATE,
          Map.of("managerId", managerId, "productId", productId));
    }
  }

  private UUID extractProductId(JoinPoint joinPoint, String paramName) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    String[] parameterNames = signature.getParameterNames();
    Object[] args = joinPoint.getArgs();
    for (int i = 0; i < parameterNames.length; i++) {
      if (parameterNames[i].equals(paramName) && args[i] instanceof UUID) {
        return (UUID) args[i];
      }
    }
    throw new IllegalArgumentException();
  }
}
