package io.github.metdaisy.amaazon.catalog.application.validator;

import io.github.metdaisy.amaazon.catalog.domain.port.out.CatalogSellerPort;
import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
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
    if (principal.getAuthorities().stream()
        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
      return;
    }
    validateSeller(principal);
  }

  private void validateSeller(AmaazonPrincipal principal) {
    UUID managerId = principal.getId();
    if (!sellerPort.existsActiveSellerByUserId(managerId)) {
      throw new AccessDeniedException("활성 판매자 승인이 필요합니다.");
    }
  }
}
