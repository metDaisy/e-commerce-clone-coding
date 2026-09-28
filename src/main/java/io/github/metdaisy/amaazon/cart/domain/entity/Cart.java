package io.github.metdaisy.amaazon.cart.domain.entity;

import io.github.metdaisy.amaazon.cart.domain.exception.CartErrorCode;
import io.github.metdaisy.amaazon.cart.domain.exception.CartException;
import io.github.metdaisy.amaazon.common.jpa.MutableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "carts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends MutableEntity {

  public static final int MAX_ITEM_TYPES = 50;
  public static final int MAX_TOTAL_QUANTITY = 1000;
  public static final Duration GUEST_LIFETIME = Duration.ofDays(30);

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true,
      fetch = FetchType.LAZY)
  private List<CartItem> items = new ArrayList<>();

  private Cart(UUID id, UUID userId, Instant expiresAt) {
    super(id);
    this.userId = userId;
    this.expiresAt = expiresAt;
  }

  public static Cart member(UUID userId) {
    if (userId == null) {
      throw new IllegalArgumentException("userId must not be null");
    }
    return new Cart(userId, userId, null);
  }

  public static Cart guest(UUID id, Clock clock) {
    if (id == null || clock == null) {
      throw new IllegalArgumentException("guest cart id and clock must not be null");
    }
    return new Cart(id, null, Instant.now(clock).plus(GUEST_LIFETIME));
  }

  public boolean isGuest() {
    return userId == null;
  }

  public boolean isExpired(Instant now) {
    return isGuest() && expiresAt != null && !expiresAt.isAfter(now);
  }

  public void renewGuest(Clock clock) {
    if (isGuest()) {
      expiresAt = Instant.now(clock).plus(GUEST_LIFETIME);
    }
  }

  public CartItem findItem(UUID offerId) {
    return items.stream().filter(item -> item.hasOffer(offerId)).findFirst().orElse(null);
  }

  public CartItem addItem(UUID offerId, int quantity) {
    requirePositiveQuantity(quantity);
    CartItem existing = findItem(offerId);
    if (existing != null) {
      int resultingQuantity = existing.getQuantity() + quantity;
      requireTotalQuantity(totalQuantity() - existing.getQuantity() + resultingQuantity);
      existing.changeQuantity(resultingQuantity);
      return existing;
    }
    if (items.size() >= MAX_ITEM_TYPES) {
      throw new CartException(CartErrorCode.CART_ITEM_LIMIT);
    }
    requireTotalQuantity(totalQuantity() + quantity);
    CartItem item = CartItem.create(this, offerId, quantity);
    items.add(item);
    return item;
  }

  public void changeQuantity(UUID itemId, int quantity) {
    CartItem item = findItemById(itemId);
    if (quantity == 0) {
      items.remove(item);
      return;
    }
    requirePositiveQuantity(quantity);
    requireTotalQuantity(totalQuantity() - item.getQuantity() + quantity);
    item.changeQuantity(quantity);
  }

  public void removeItem(UUID itemId) {
    items.remove(findItemById(itemId));
  }

  public void clearItems() {
    items.clear();
  }

  public int totalQuantity() {
    return items.stream().mapToInt(CartItem::getQuantity).sum();
  }

  private CartItem findItemById(UUID itemId) {
    return items.stream().filter(item -> item.getId().equals(itemId)).findFirst()
        .orElseThrow(() -> new CartException(CartErrorCode.CART_NOT_FOUND));
  }

  private void requireTotalQuantity(int quantity) {
    if (quantity > MAX_TOTAL_QUANTITY) {
      throw new CartException(CartErrorCode.CART_QUANTITY_LIMIT);
    }
  }

  private void requirePositiveQuantity(int quantity) {
    if (quantity < 1) {
      throw new CartException(CartErrorCode.CART_INVALID_INPUT);
    }
  }
}
