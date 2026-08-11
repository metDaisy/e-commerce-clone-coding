package io.github.metdaisy.amaazon.catalog.application.validator;

import io.github.metdaisy.amaazon.catalog.application.port.out.CatalogSellerPort;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
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
public class ActiveSellerValidator {

  private final CatalogSellerPort sellerPort;
  private final CatalogProductRepository repository;

  @Before("@annotation(activeSeller)")
  public void validate(JoinPoint point, ActiveSeller activeSeller) {
    AmaazonPrincipal principal = (AmaazonPrincipal) SecurityContextHolder.getContext()
        .getAuthentication().getPrincipal();
    if ("ADMIN".equals(principal.getRole())) {
      return;
    }
    validateSeller(principal);
    if (activeSeller.checkOwner()) {
      validateOwner(point, activeSeller, principal);
    }
  }

  private void validateOwner(JoinPoint joinPoint, ActiveSeller activeSeller,
      AmaazonPrincipal principal) {
    UUID catalogId = extractCatalogId(joinPoint, activeSeller.catalogId());
    UUID managerId = principal.getId();
    if (!repository.existsByIdAndManagerId(catalogId, managerId)) {
      throw new CatalogProductException(CatalogProductErrorCode.UNAUTHORIZED_UPDATE,
          Map.of("catalogId", catalogId, "managerId", managerId));
    }
  }

  private void validateSeller(AmaazonPrincipal principal) {
    UUID managerId = principal.getId();
    if (!sellerPort.existsSeller(managerId)) {
      throw new CatalogProductException(CatalogProductErrorCode.SELLER_APPROVAL_REQUIRED,
          Map.of("managerId", managerId));
    }
  }

  private UUID extractCatalogId(JoinPoint joinPoint, String paramName) {
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
