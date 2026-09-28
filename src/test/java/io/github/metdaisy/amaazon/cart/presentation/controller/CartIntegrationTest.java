package io.github.metdaisy.amaazon.cart.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.metdaisy.amaazon.cart.application.dto.CartMergeResult;
import io.github.metdaisy.amaazon.cart.application.port.in.CartMergeApi;
import io.github.metdaisy.amaazon.cart.application.port.out.OfferQueryPort;
import io.github.metdaisy.amaazon.cart.domain.entity.Cart;
import io.github.metdaisy.amaazon.cart.domain.repository.CartRepository;
import io.github.metdaisy.amaazon.global.security.jwt.model.JwtPrincipal;
import io.github.metdaisy.amaazon.support.BaseIntegrationTest;
import io.github.metdaisy.amaazon.user.application.port.in.UserQueryApi;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DisplayName("Cart HTTP 통합 테스트")
class CartIntegrationTest extends BaseIntegrationTest {

  private static final String CART_URL = "/api/v1/cart";
  private static final UUID USER_ID =
      UUID.fromString("2bb8df7f-9478-4d51-b055-496016dd421f");

  @Autowired
  private CartMergeApi cartMergeApi;

  @Autowired
  private CartRepository cartRepository;

  @MockitoBean
  private OfferQueryPort offerQueryPort;

  @MockitoBean
  private UserQueryApi userQueryApi;

  @Test
  @DisplayName("Cart 조회: DB의 항목을 현재 Offer 가격과 판매 상태로 보강하고 품절·판매중지 항목을 보존한다")
  void getCart_enrichesItemsWithoutRemovingUnavailableOffers() throws Exception {
    UUID activeOfferId = UUID.randomUUID();
    UUID inactiveOfferId = UUID.randomUUID();
    UUID outOfStockOfferId = UUID.randomUUID();
    Cart cart = Cart.member(USER_ID);
    cart.addItem(activeOfferId, 2);
    cart.addItem(inactiveOfferId, 1);
    cart.addItem(outOfStockOfferId, 1);
    persistAndFlush(cart);
    clear();

    given(offerQueryPort.findById(activeOfferId)).willReturn(Optional.of(snapshot(
        activeOfferId, "ACTIVE", BigDecimal.valueOf(12000), 20)));
    given(offerQueryPort.findById(inactiveOfferId)).willReturn(Optional.of(snapshot(
        inactiveOfferId, "INACTIVE", BigDecimal.valueOf(8000), 20)));
    given(offerQueryPort.findById(outOfStockOfferId)).willReturn(Optional.of(snapshot(
        outOfStockOfferId, "ACTIVE", BigDecimal.valueOf(7000), 0)));
    willDoNothing().given(userQueryApi).requireEnabled(USER_ID);

    mockMvc.perform(get(CART_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAs(USER_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cartId").value(USER_ID.toString()))
        .andExpect(jsonPath("$.itemCount").value(3))
        .andExpect(jsonPath("$.totalQuantity").value(4))
        .andExpect(jsonPath("$.items[*].unitPrice.amount")
            .value(containsInAnyOrder(12000, 8000, 7000)))
        .andExpect(jsonPath("$.items[*].unavailable")
            .value(containsInAnyOrder(false, true, false)))
        .andExpect(jsonPath("$.items[*].outOfStock")
            .value(containsInAnyOrder(false, false, true)))
        .andExpect(jsonPath("$.total.amount").value(39000));
  }

  @Test
  @DisplayName("Cart 병합 공개 seam: 비회원 항목을 회원 Cart로 옮기고 원본 Cart를 삭제한다")
  void mergeGuestCart_movesItemsAtomically() {
    UUID guestCartId = UUID.randomUUID();
    UUID offerId = UUID.randomUUID();
    Cart guest = Cart.guest(guestCartId, Clock.systemUTC());
    guest.addItem(offerId, 2);
    persistAndFlush(guest);
    clear();
    given(offerQueryPort.findById(offerId)).willReturn(Optional.of(snapshot(
        offerId, "ACTIVE", BigDecimal.valueOf(12000), 20)));

    CartMergeResult result = cartMergeApi.merge(USER_ID, guestCartId);
    flushAndClear();

    assertThat(result.status()).isEqualTo(CartMergeResult.Status.MERGED);
    assertThat(cartRepository.findGuestById(guestCartId)).isEmpty();
    assertThat(cartRepository.findByUserId(USER_ID).orElseThrow().totalQuantity()).isEqualTo(2);
  }

  private OfferQueryPort.OfferSnapshot snapshot(
      UUID offerId, String status, BigDecimal price, int availableQuantity) {
    return new OfferQueryPort.OfferSnapshot(offerId, "상품", null, price, "KRW", status, 10,
        availableQuantity);
  }

  private Authentication authenticationAs(UUID userId) {
    JwtPrincipal principal = new JwtPrincipal(userId, "USER");
    return new UsernamePasswordAuthenticationToken(
        principal, null, principal.getAuthorities());
  }
}
