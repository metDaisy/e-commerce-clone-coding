package io.github.metdaisy.amaazon.seller.application.port.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import io.github.metdaisy.amaazon.seller.domain.entity.constant.SellerStatus;
import io.github.metdaisy.amaazon.seller.domain.repository.SellerRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("판매자 조회 API")
class SellerQueryApiTest {

  @Mock
  private SellerRepository repository;

  @InjectMocks
  private SellerQueryApi api;

  @Test
  @DisplayName("활성 판매자 확인: 판매자 ID가 아닌 사용자 ID와 ACTIVE 상태로 조회한다")
  void isActiveSeller_shouldQueryByUserIdAndActiveStatus() {
    UUID userId = UUID.randomUUID();
    given(repository.existsByUserIdAndStatus(userId, SellerStatus.ACTIVE)).willReturn(true);

    assertThat(api.isActiveSeller(userId)).isTrue();

    then(repository).should().existsByUserIdAndStatus(userId, SellerStatus.ACTIVE);
  }
}
