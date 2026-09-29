package io.github.metdaisy.amaazon.cart.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import io.github.metdaisy.amaazon.cart.application.dto.AddCartItemRequest;
import io.github.metdaisy.amaazon.cart.application.dto.ChangeCartItemQuantityRequest;
import io.github.metdaisy.amaazon.cart.application.dto.CartMergeResult;
import io.github.metdaisy.amaazon.cart.application.port.out.OfferQueryPort;
import io.github.metdaisy.amaazon.cart.domain.entity.Cart;
import io.github.metdaisy.amaazon.cart.domain.exception.CartErrorCode;
import io.github.metdaisy.amaazon.cart.domain.repository.CartRepository;
import io.github.metdaisy.amaazon.common.exception.AmaazonException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cart application service")
class CartServiceTest {

  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
  private static final UUID USER_ID = UUID.randomUUID();
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Mock
  private CartRepository cartRepository;

  @Mock
  private OfferQueryPort offerQueryPort;

  private CartService cartService;

  @BeforeEach
  void setUp() {
    cartService = new CartService(cartRepository, offerQueryPort, CLOCK);
  }

  @Test
  @DisplayName("상품 추가: Offer port가 비어 있으면 P9 OFFER-001을 유지한다")
  void addItem_preservesOfferNotFoundCode() {
    UUID offerId = UUID.randomUUID();
    given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
    given(cartRepository.save(any(Cart.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(offerQueryPort.findById(offerId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> cartService.addItem(USER_ID, null,
        new AddCartItemRequest(offerId, 1)))
        .isInstanceOf(AmaazonException.class)
        .extracting("code")
        .isEqualTo("OFFER-001");
  }

  @Test
  @DisplayName("수량 변경: Offer port가 비어 있으면 P9 OFFER-001을 유지한다")
  void changeQuantity_preservesOfferNotFoundCode() {
    UUID offerId = UUID.randomUUID();
    Cart cart = Cart.member(USER_ID);
    var item = cart.addItem(offerId, 1);
    given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));
    given(offerQueryPort.findById(offerId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> cartService.changeQuantity(USER_ID, item.getId(),
        new ChangeCartItemQuantityRequest(2)))
        .isInstanceOf(AmaazonException.class)
        .extracting("code")
        .isEqualTo("OFFER-001");
    assertThat(item.getQuantity()).isEqualTo(1);
  }

  @Test
  @DisplayName("Cart 병합: Offer port가 비어 있으면 양쪽 Cart를 보존하고 P9 OFFER-001을 반환한다")
  void mergeGuestCart_preservesStateForMissingOffer() {
    UUID guestCartId = UUID.randomUUID();
    UUID offerId = UUID.randomUUID();
    Cart guest = Cart.guest(guestCartId, CLOCK);
    guest.addItem(offerId, 1);
    given(cartRepository.findGuestById(guestCartId)).willReturn(Optional.of(guest));
    given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(Cart.member(USER_ID)));
    given(offerQueryPort.findById(offerId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> cartService.mergeGuestCart(USER_ID, guestCartId))
        .isInstanceOf(AmaazonException.class)
        .extracting("code")
        .isEqualTo("OFFER-001");
    then(cartRepository).should(never()).save(any(Cart.class));
    then(cartRepository).should(never()).delete(any(Cart.class));
    assertThat(guest.getItems()).hasSize(1);
  }

  @Test
  @DisplayName("Cart 병합: P3 제한 충돌은 CART-007 결과로 반환하고 Cart를 변경하지 않는다")
  void mergeGuestCart_returnsCartConflictForLimit() {
    UUID guestCartId = UUID.randomUUID();
    UUID offerId = UUID.randomUUID();
    Cart guest = Cart.guest(guestCartId, CLOCK);
    guest.addItem(offerId, 2);
    Cart member = Cart.member(USER_ID);
    given(cartRepository.findGuestById(guestCartId)).willReturn(Optional.of(guest));
    given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(member));
    given(offerQueryPort.findById(offerId)).willReturn(Optional.of(snapshot(offerId, 1)));

    CartMergeResult result = cartService.mergeGuestCart(USER_ID, guestCartId);

    assertThat(result.status()).isEqualTo(CartMergeResult.Status.CONFLICT);
    assertThat(result.exceptionCode()).isEqualTo(CartErrorCode.CART_MERGE_CONFLICT.getCode());
    assertThat(result.conflictOfferIds()).containsExactly(offerId);
    then(cartRepository).should(never()).save(any(Cart.class));
    then(cartRepository).should(never()).delete(any(Cart.class));
    assertThat(member.getItems()).isEmpty();
  }

  @Test
  @DisplayName("만료된 비회원 Cart 병합: 원본 Cart를 폐기하고 빈 병합 성공을 반환한다")
  void mergeGuestCart_discardsExpiredGuest() {
    UUID guestCartId = UUID.randomUUID();
    Cart expiredGuest = Cart.guest(guestCartId,
        Clock.fixed(NOW.minus(Cart.GUEST_LIFETIME).minusSeconds(1), ZoneOffset.UTC));
    given(cartRepository.findGuestById(guestCartId)).willReturn(Optional.of(expiredGuest));

    CartMergeResult result = cartService.mergeGuestCart(USER_ID, guestCartId);

    assertThat(result.status()).isEqualTo(CartMergeResult.Status.MERGED);
    then(cartRepository).should().delete(expiredGuest);
  }

  @Test
  @DisplayName("소유자 Cart가 없으면 수량 변경을 Cart-001로 거절한다")
  void changeQuantity_rejectsUnknownOwner() {
    given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

    assertThatThrownBy(() -> cartService.changeQuantity(USER_ID, UUID.randomUUID(),
        new ChangeCartItemQuantityRequest(1)))
        .isInstanceOf(AmaazonException.class)
        .extracting("code")
        .isEqualTo(CartErrorCode.CART_NOT_FOUND.getCode());
  }

  @Test
  @DisplayName("결제 완료 정리: 전달된 Cart Item만 삭제하고 빈 재전달은 멱등적으로 무시한다")
  void cleanupAfterPayment_isIdempotentAndScopedToIds() {
    UUID itemId = UUID.randomUUID();
    Collection<UUID> itemIds = List.of(itemId);
    given(cartRepository.deleteItemsByIds(itemIds)).willReturn(1, 0);

    assertThat(cartService.cleanupAfterPayment(itemIds)).isOne();
    assertThat(cartService.cleanupAfterPayment(itemIds)).isZero();
    assertThat(cartService.cleanupAfterPayment(List.of())).isZero();
    then(cartRepository).should(times(2)).deleteItemsByIds(itemIds);
  }

  private OfferQueryPort.OfferSnapshot snapshot(UUID offerId, int maxPurchaseQuantity) {
    return new OfferQueryPort.OfferSnapshot(offerId, "Offer", null,
        BigDecimal.TEN, "KRW", "ACTIVE", maxPurchaseQuantity, 10);
  }
}
