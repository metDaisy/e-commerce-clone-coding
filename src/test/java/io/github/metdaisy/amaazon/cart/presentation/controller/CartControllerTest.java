package io.github.metdaisy.amaazon.cart.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.metdaisy.amaazon.cart.application.dto.AddCartItemRequest;
import io.github.metdaisy.amaazon.cart.application.dto.ChangeCartItemQuantityRequest;
import io.github.metdaisy.amaazon.cart.application.port.out.OfferNotFoundException;
import io.github.metdaisy.amaazon.cart.application.service.CartService;
import io.github.metdaisy.amaazon.cart.domain.exception.CartErrorCode;
import io.github.metdaisy.amaazon.cart.domain.exception.CartException;
import io.github.metdaisy.amaazon.cart.presentation.cookie.GuestCartCookieProvider;
import io.github.metdaisy.amaazon.support.RestControllerTest;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Cart 컨트롤러 슬라이스 테스트")
class CartControllerTest extends RestControllerTest {

  private static final String CART_URL = API_PREFIX + "/cart";

  @MockitoBean
  private CartService cartService;

  @MockitoBean
  private GuestCartCookieProvider cookieProvider;

  @Test
  @DisplayName("비회원 상품 추가 성공: 204와 30일 guest_cart_id 쿠키를 반환한다")
  void addItem_guest_returnsCookie() throws Exception {
    UUID cartId = UUID.randomUUID();
    UUID offerId = UUID.randomUUID();
    AddCartItemRequest request = new AddCartItemRequest(offerId, 2);
    ResponseCookie cookie = ResponseCookie.from(GuestCartCookieProvider.NAME, cartId.toString())
        .httpOnly(true)
        .sameSite("Lax")
        .path("/")
        .maxAge(2592000)
        .build();
    SecurityContextHolder.clearContext();
    given(cartService.addItem(eq(null), eq(null), eq(request))).willReturn(cartId);
    given(cookieProvider.create(cartId.toString())).willReturn(cookie);

    mockMvc.perform(post(CART_URL + "/items").contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent())
        .andExpect(header().string("Set-Cookie", cookie.toString()));
    then(cartService).should().addItem(eq(null), eq(null), eq(request));
  }

  @Test
  @DisplayName("회원 수량 변경 성공: 인증 주체와 Cart Item을 서비스에 전달하고 204를 반환한다")
  void changeQuantity_member_delegatesAndReturnsNoContent() throws Exception {
    UUID itemId = UUID.randomUUID();
    ChangeCartItemQuantityRequest request = new ChangeCartItemQuantityRequest(3);

    mockMvc.perform(patch(CART_URL + "/items/" + itemId)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent());
    then(cartService).should().changeQuantity(eq(USER_ID), eq(itemId), eq(request));
  }

  @Test
  @DisplayName("상품 추가 실패: Offer 원본 예외 코드 OFFER-001을 HTTP 404로 전달한다")
  void addItem_missingOffer_preservesOfferError() throws Exception {
    AddCartItemRequest request = new AddCartItemRequest(UUID.randomUUID(), 1);
    given(cartService.addItem(eq(USER_ID), any(), eq(request)))
        .willThrow(new OfferNotFoundException(request.offerId()));

    mockMvc.perform(post(CART_URL + "/items").contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.exceptionCode").value("OFFER-001"));
  }

  @Test
  @DisplayName("Cart 입력 실패: 서비스의 CART-002를 HTTP 400으로 전달한다")
  void addItem_invalidInput_mapsCartError() throws Exception {
    AddCartItemRequest request = new AddCartItemRequest(UUID.randomUUID(), 0);
    given(cartService.addItem(eq(USER_ID), any(), eq(request)))
        .willThrow(new CartException(CartErrorCode.CART_INVALID_INPUT));

    mockMvc.perform(post(CART_URL + "/items").contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionCode").value("CART-002"));
  }
}
