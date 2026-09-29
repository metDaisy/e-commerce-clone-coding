package io.github.metdaisy.amaazon.cart.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.metdaisy.amaazon.cart.domain.entity.Cart;
import io.github.metdaisy.amaazon.cart.domain.entity.CartItem;
import io.github.metdaisy.amaazon.support.BaseRepositoryTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Cart JPA repository")
class CartJpaRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private CartJpaRepository cartRepository;

  @Test
  @DisplayName("회원 Cart 조회: Cart Item을 함께 읽고 사용자 소유 Cart를 하나로 조회한다")
  void findByUserId_fetchesItems() {
    UUID userId = UUID.randomUUID();
    Cart cart = Cart.member(userId);
    CartItem item = cart.addItem(UUID.randomUUID(), 2);
    persistAndFlush(cart);
    clear();

    Cart loaded = cartRepository.findByUserId(userId).orElseThrow();

    assertThat(loaded.getId()).isEqualTo(userId);
    assertThat(loaded.getItems()).extracting(CartItem::getId).containsExactly(item.getId());
    assertThat(loaded.getItems().get(0).getQuantity()).isEqualTo(2);
  }

  @Test
  @DisplayName("결제 Cart Item 정리: 지정한 ID만 삭제하고 같은 요청을 다시 실행해도 0건이다")
  void deleteItemsByIds_isScopedAndIdempotent() {
    Cart cart = Cart.member(UUID.randomUUID());
    CartItem selected = cart.addItem(UUID.randomUUID(), 1);
    CartItem retained = cart.addItem(UUID.randomUUID(), 1);
    persistAndFlush(cart);
    clear();

    assertThat(cartRepository.deleteItemsByIds(List.of(selected.getId()))).isOne();
    em.flush();
    assertThat(cartRepository.deleteItemsByIds(List.of(selected.getId()))).isZero();
    em.flush();
    clear();

    Cart loaded = cartRepository.findByUserId(cart.getUserId()).orElseThrow();
    assertThat(loaded.getItems()).extracting(CartItem::getId).containsExactly(retained.getId());
  }
}
