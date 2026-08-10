package io.github.metdaisy.amaazon.catalog.application.validator;

import io.github.metdaisy.amaazon.catalog.application.port.out.CatalogSellerPort;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class ActiveSellerValidator {

  private final CatalogSellerPort sellerPort;

  @Before("@annotation(activeSeller)")
  public void validate(JoinPoint point, ActiveSeller activeSeller) {
    AmaazonPrincipal principal = (AmaazonPrincipal) SecurityContextHolder.getContext()
        .getAuthentication().getPrincipal();
    if (principal.getRole().equals("ADMIN")) {
      return;
    }
    UUID managerId = principal.getId();
    if (!sellerPort.existsSeller(managerId)) {
      throw new CatalogProductException(CatalogProductErrorCode.SELLER_APPROVAL_REQUIRED,
          Map.of("managerId", managerId));
    }
  }
}
