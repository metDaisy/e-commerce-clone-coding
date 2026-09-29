package io.github.metdaisy.amaazon.cart.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.metdaisy.amaazon.cart.domain.exception.CartErrorCode;
import io.github.metdaisy.amaazon.cart.domain.exception.CartException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Cart 도메인")
class CartTest {

  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  @DisplayName("회원 Cart는 사용자 ID를 식별자로 사용하고 만료되지 않는다")
  void memberCartHasStableOwnership() {
    UUID userId = UUID.randomUUID();

    Cart cart = Cart.member(userId);

    assertThat(cart.getId()).isEqualTo(userId);
    assertThat(cart.getUserId()).isEqualTo(userId);
    assertThat(cart.getExpiresAt()).isNull();
    assertThat(cart.isExpired(NOW.plusSeconds(1))).isFalse();
  }

  @Test
  @DisplayName("비회원 Cart는 생성 시 30일 만료를 갖고 갱신 시점을 30일 연장한다")
  void guestCartExpiresAndRenews() {
    Cart cart = Cart.guest(UUID.randomUUID(), CLOCK);

    assertThat(cart.getExpiresAt()).isEqualTo(NOW.plus(Cart.GUEST_LIFETIME));
    cart.renewGuest(Clock.offset(CLOCK, Duration.ofDays(1)));
    assertThat(cart.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(31)));
  }

  @Test
  @DisplayName("동일 Offer는 합산되고 quantity 0 변경은 항목을 삭제한다")
  void sameOfferIsMergedAndZeroDeletes() {
    Cart cart = Cart.member(UUID.randomUUID());
    UUID offerId = UUID.randomUUID();

    CartItem first = cart.addItem(offerId, 2);
    CartItem second = cart.addItem(offerId, 3);
    cart.changeQuantity(first.getId(), 0);

    assertThat(second).isSameAs(first);
    assertThat(cart.getItems()).isEmpty();
  }

  @Test
  @DisplayName("서로 다른 Offer 종류가 50개를 초과하면 기존 항목을 보존하고 거절한다")
  void itemTypeLimitIsAtomic() {
    Cart cart = Cart.member(UUID.randomUUID());
    for (int index = 0; index < Cart.MAX_ITEM_TYPES; index++) {
      cart.addItem(UUID.randomUUID(), 1);
    }

    assertThatThrownBy(() -> cart.addItem(UUID.randomUUID(), 1))
        .isInstanceOf(CartException.class)
        .extracting("code")
        .isEqualTo(CartErrorCode.CART_ITEM_LIMIT.getCode());
    assertThat(cart.getItems()).hasSize(Cart.MAX_ITEM_TYPES);
  }

  @Test
  @DisplayName("전체 수량 1000개 초과는 기존 Cart 상태를 변경하지 않고 거절한다")
  void totalQuantityLimitIsAtomic() {
    Cart cart = Cart.member(UUID.randomUUID());
    UUID offerId = UUID.randomUUID();
    cart.addItem(offerId, 1000);

    assertThatThrownBy(() -> cart.addItem(UUID.randomUUID(), 1))
        .isInstanceOf(CartException.class)
        .extracting("code")
        .isEqualTo(CartErrorCode.CART_QUANTITY_LIMIT.getCode());
    assertThat(cart.totalQuantity()).isEqualTo(1000);
  }
}
