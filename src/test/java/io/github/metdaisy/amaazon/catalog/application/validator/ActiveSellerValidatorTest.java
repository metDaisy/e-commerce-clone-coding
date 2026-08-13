package io.github.metdaisy.amaazon.catalog.application.validator;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.catalog.domain.port.out.CatalogSellerPort;
import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import java.util.UUID;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("활성 판매자 검증기")
class ActiveSellerValidatorTest {

  @Mock
  private CatalogSellerPort sellerPort;

  @Mock
  private AmaazonPrincipal principal;

  @Mock
  private JoinPoint joinPoint;

  @Mock
  private ActiveSeller activeSeller;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("판매자 검증: 관리자는 판매자 상태 조회 없이 통과한다")
  void validate_shouldAllowAdminWithoutSellerLookup() {
    authenticateAs("ADMIN", UUID.randomUUID());

    new ActiveSellerValidator(sellerPort).validate(joinPoint, activeSeller);

    then(sellerPort).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("판매자 검증: 활성 판매자는 통과한다")
  void validate_shouldAllowActiveSeller() {
    UUID sellerId = UUID.randomUUID();
    authenticateAs("PRODUCT_MANAGER", sellerId);
    given(sellerPort.existsActiveSellerByUserId(sellerId)).willReturn(true);

    new ActiveSellerValidator(sellerPort).validate(joinPoint, activeSeller);

    then(sellerPort).should().existsActiveSellerByUserId(sellerId);
  }

  @Test
  @DisplayName("판매자 검증 실패: 비활성 판매자는 승인 필요 오류를 반환한다")
  void validate_shouldRejectInactiveSeller() {
    UUID sellerId = UUID.randomUUID();
    authenticateAs("PRODUCT_MANAGER", sellerId);
    given(sellerPort.existsActiveSellerByUserId(sellerId)).willReturn(false);

    assertThatThrownBy(() -> new ActiveSellerValidator(sellerPort)
        .validate(joinPoint, activeSeller))
        .isInstanceOf(CatalogProductException.class)
        .hasFieldOrPropertyWithValue("code",
            CatalogProductErrorCode.SELLER_APPROVAL_REQUIRED.getCode());
  }

  private void authenticateAs(String role, UUID id) {
    given(principal.getRole()).willReturn(role);
    if (!"ADMIN".equals(role)) {
      given(principal.getId()).willReturn(id);
    }
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null));
  }
}
