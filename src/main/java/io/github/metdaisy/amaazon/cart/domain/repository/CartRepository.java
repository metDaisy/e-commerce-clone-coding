package io.github.metdaisy.amaazon.cart.domain.repository;

import io.github.metdaisy.amaazon.cart.domain.entity.Cart;
import io.github.metdaisy.amaazon.cart.domain.entity.CartItem;
import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends DomainRepository<Cart> {

  Optional<Cart> findByUserId(UUID userId);

  Optional<Cart> findGuestById(UUID cartId);

  Optional<CartItem> findItemById(UUID itemId);

  List<Cart> findExpiredGuests(Instant now);

  int deleteItemsByIds(Collection<UUID> itemIds);

  int deleteExpiredGuests(Instant now);
}
