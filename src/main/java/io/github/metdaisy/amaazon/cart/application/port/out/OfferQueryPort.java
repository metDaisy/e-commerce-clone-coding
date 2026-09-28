package io.github.metdaisy.amaazon.cart.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

@NamedInterface("offer")
public interface OfferQueryPort {

  Optional<OfferSnapshot> findById(UUID offerId);

  record OfferSnapshot(
      UUID offerId,
      String name,
      String thumbnailUrl,
      BigDecimal currentPrice,
      String currency,
      String status,
      int maxPurchaseQuantity,
      int availableQuantity) {

    public boolean isPurchasable() {
      return "ACTIVE".equalsIgnoreCase(status);
    }

    public boolean isOutOfStock() {
      return availableQuantity <= 0;
    }
  }
}
