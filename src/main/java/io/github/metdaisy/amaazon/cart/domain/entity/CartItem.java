package io.github.metdaisy.amaazon.cart.domain.entity;

import io.github.metdaisy.amaazon.cart.domain.exception.CartErrorCode;
import io.github.metdaisy.amaazon.cart.domain.exception.CartException;
import io.github.metdaisy.amaazon.common.jpa.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "cart_items", uniqueConstraints = @UniqueConstraint(
    name = "uq_cart_items_cart_offer", columnNames = {"cart_id", "offer_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends MutableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cart_id", nullable = false)
  private Cart cart;

  @Column(name = "offer_id", nullable = false)
  private UUID offerId;

  @Column(name = "quantity", nullable = false)
  private int quantity;

  private CartItem(Cart cart, UUID offerId, int quantity) {
    super(UUID.randomUUID());
    if (cart == null || offerId == null || quantity < 1) {
      throw new CartException(CartErrorCode.CART_INVALID_INPUT);
    }
    this.cart = cart;
    this.offerId = offerId;
    this.quantity = quantity;
  }

  static CartItem create(Cart cart, UUID offerId, int quantity) {
    return new CartItem(cart, offerId, quantity);
  }

  public boolean hasOffer(UUID id) {
    return offerId.equals(id);
  }

  public void changeQuantity(int quantity) {
    if (quantity < 1) {
      throw new CartException(CartErrorCode.CART_INVALID_INPUT);
    }
    this.quantity = quantity;
  }
}
