package io.github.metdaisy.amaazon.catalog.application.validator;

import io.github.metdaisy.amaazon.catalog.domain.port.out.CatalogSellerPort;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class ActiveSellerValidator {

  private final CatalogSellerPort sellerPort;
  @Before("@annotation(activeSeller)")
  public void validate(JoinPoint point, ActiveSeller activeSeller) {
    AmaazonPrincipal principal = (AmaazonPrincipal) SecurityContextHolder.getContext()
        .getAuthentication().getPrincipal();
    if ("ADMIN".equals(principal.getRole())) {
      return;
    }
    validateSeller(principal);
  }

  private void validateSeller(AmaazonPrincipal principal) {
    UUID managerId = principal.getId();
    if (!sellerPort.existsActiveSellerByUserId(managerId)) {
      throw new CatalogProductException(CatalogProductErrorCode.SELLER_APPROVAL_REQUIRED,
          Map.of("managerId", managerId));
    }
  }
}
