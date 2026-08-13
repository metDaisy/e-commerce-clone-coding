package io.github.metdaisy.amaazon.catalog.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willReturn;

import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogProductIdentifierType;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@DisplayName("카탈로그 상품 JPA 저장소")
class CatalogProductJpaRepositoryTest {

  @Mock(answer = Answers.CALLS_REAL_METHODS)
  private CatalogProductJpaRepository repository;

  @Test
  @DisplayName("식별자 존재 확인: 상품 ID와 식별자 조건을 결합한 명세로 조회한다")
  void existsIdentifier_shouldDelegateCombinedSpecification() {
    UUID productId = UUID.randomUUID();
    willReturn(true).given(repository).exists(ArgumentMatchers
        .<Specification<CatalogProduct>>any());

    assertThat(repository.existsIdentifier(productId, CatalogProductIdentifierType.ASIN,
        "B000123456")).isTrue();
  }
}
