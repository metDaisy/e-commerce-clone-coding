package io.github.metdaisy.amaazon.cart.application.port.in;

import io.github.metdaisy.amaazon.cart.application.service.CartService;
import java.util.Collection;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@NamedInterface("api")
@Component
@RequiredArgsConstructor
public class CartPaymentCleanupApi {

  private final CartService cartService;

  @Transactional
  public int cleanup(Collection<UUID> cartItemIds) {
    return cartService.cleanupAfterPayment(cartItemIds);
  }
}
