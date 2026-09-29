package io.github.metdaisy.amaazon.cart.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(
    UUID cartId,
    List<CartItemResponse> items,
    int itemCount,
    int totalQuantity,
    Money subtotal,
    Money total) {

  public CartResponse {
    items = List.copyOf(items);
  }

  public record CartItemResponse(
      UUID cartItemId,
      UUID offerId,
      String name,
      String thumbnailUrl,
      int quantity,
      Money unitPrice,
      Money subtotal,
      boolean outOfStock,
      boolean unavailable) {
  }

  public record Money(BigDecimal amount, String currency) {
  }
}
