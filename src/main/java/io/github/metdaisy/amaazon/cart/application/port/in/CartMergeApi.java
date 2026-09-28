package io.github.metdaisy.amaazon.cart.application.port.in;

import io.github.metdaisy.amaazon.cart.application.dto.CartMergeResult;
import io.github.metdaisy.amaazon.cart.application.service.CartService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@NamedInterface("api")
@Component
@RequiredArgsConstructor
public class CartMergeApi {

  private final CartService cartService;

  @Transactional
  public CartMergeResult merge(UUID userId, UUID guestCartId) {
    return cartService.mergeGuestCart(userId, guestCartId);
  }
}
