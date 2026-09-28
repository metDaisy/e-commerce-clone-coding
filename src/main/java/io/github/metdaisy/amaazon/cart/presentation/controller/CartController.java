package io.github.metdaisy.amaazon.cart.presentation.controller;

import io.github.metdaisy.amaazon.cart.application.dto.AddCartItemRequest;
import io.github.metdaisy.amaazon.cart.application.dto.CartResponse;
import io.github.metdaisy.amaazon.cart.application.dto.ChangeCartItemQuantityRequest;
import io.github.metdaisy.amaazon.cart.application.service.CartService;
import io.github.metdaisy.amaazon.cart.domain.exception.CartErrorCode;
import io.github.metdaisy.amaazon.cart.domain.exception.CartException;
import io.github.metdaisy.amaazon.cart.presentation.cookie.GuestCartCookieProvider;
import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import io.github.metdaisy.amaazon.common.auth.RequireEnabledUser;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

  private final CartService cartService;
  private final GuestCartCookieProvider cookieProvider;

  @GetMapping
  @RequireEnabledUser
  public CartResponse getCart(@AuthenticationPrincipal AmaazonPrincipal principal) {
    return cartService.getCart(principal.getId());
  }

  @PostMapping("/items")
  public ResponseEntity<Void> addItem(
      @AuthenticationPrincipal AmaazonPrincipal principal,
      @CookieValue(value = GuestCartCookieProvider.NAME, required = false) String guestCartId,
      @RequestBody @Valid AddCartItemRequest request) {
    UUID userId = userId(principal);
    UUID existingGuestId = parseGuestCartId(guestCartId);
    UUID cartId = cartService.addItem(userId, existingGuestId, request);
    ResponseEntity.HeadersBuilder<?> response = ResponseEntity.noContent();
    if (userId == null) {
      response.header(HttpHeaders.SET_COOKIE, cookieProvider.create(cartId.toString()).toString());
    }
    return response.build();
  }

  @PatchMapping("/items/{cartItemId}")
  @RequireEnabledUser
  public ResponseEntity<Void> changeQuantity(
      @AuthenticationPrincipal AmaazonPrincipal principal,
      @PathVariable String cartItemId,
      @RequestBody @Valid ChangeCartItemQuantityRequest request) {
    cartService.changeQuantity(principal.getId(), parseRequiredUuid(cartItemId), request);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/items/{cartItemId}")
  @RequireEnabledUser
  public ResponseEntity<Void> deleteItem(
      @AuthenticationPrincipal AmaazonPrincipal principal,
      @PathVariable String cartItemId) {
    cartService.deleteItem(principal.getId(), parseRequiredUuid(cartItemId));
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/items")
  public ResponseEntity<Void> clear(
      @AuthenticationPrincipal AmaazonPrincipal principal,
      @CookieValue(value = GuestCartCookieProvider.NAME, required = false) String guestCartId) {
    UUID userId = userId(principal);
    cartService.clear(userId, parseGuestCartId(guestCartId));
    ResponseEntity.HeadersBuilder<?> response = ResponseEntity.noContent();
    if (userId == null) {
      response.header(HttpHeaders.SET_COOKIE, cookieProvider.delete().toString());
    }
    return response.build();
  }

  private UUID userId(AmaazonPrincipal principal) {
    return principal == null ? null : principal.getId();
  }

  private UUID parseRequiredUuid(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException exception) {
      throw new CartException(CartErrorCode.CART_INVALID_INPUT);
    }
  }

  private UUID parseGuestCartId(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }
}
