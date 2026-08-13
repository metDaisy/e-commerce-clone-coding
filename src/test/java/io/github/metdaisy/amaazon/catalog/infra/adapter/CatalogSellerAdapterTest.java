package io.github.metdaisy.amaazon.catalog.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import io.github.metdaisy.amaazon.seller.application.port.in.SellerQueryApi;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("카탈로그 판매자 어댑터")
class CatalogSellerAdapterTest {

  @Mock
  private SellerQueryApi api;

  @InjectMocks
  private CatalogSellerAdapter adapter;

  @Test
  @DisplayName("판매자 존재 확인: 판매자 조회 API의 활성 상태를 그대로 반환한다")
  void existsSeller_shouldDelegateToSellerQueryApi() {
    UUID sellerId = UUID.randomUUID();
    given(api.isActiveSeller(sellerId)).willReturn(true);

    assertThat(adapter.existsSeller(sellerId)).isTrue();
  }
}
