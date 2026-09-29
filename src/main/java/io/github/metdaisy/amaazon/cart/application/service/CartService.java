package io.github.metdaisy.amaazon.cart.application.service;

import io.github.metdaisy.amaazon.cart.application.dto.AddCartItemRequest;
import io.github.metdaisy.amaazon.cart.application.dto.CartMergeResult;
import io.github.metdaisy.amaazon.cart.application.dto.CartResponse;
import io.github.metdaisy.amaazon.cart.application.port.out.OfferNotFoundException;
import io.github.metdaisy.amaazon.cart.application.port.out.OfferQueryPort;
import io.github.metdaisy.amaazon.cart.domain.entity.Cart;
import io.github.metdaisy.amaazon.cart.domain.entity.CartItem;
import io.github.metdaisy.amaazon.cart.domain.exception.CartErrorCode;
import io.github.metdaisy.amaazon.cart.domain.exception.CartException;
import io.github.metdaisy.amaazon.cart.domain.repository.CartRepository;
import io.github.metdaisy.amaazon.cart.application.dto.ChangeCartItemQuantityRequest;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

  private final CartRepository cartRepository;
  private final OfferQueryPort offerQueryPort;
  private final Clock clock;

  @Transactional
  public UUID addItem(UUID userId, UUID guestCartId, AddCartItemRequest request) {
    validateAddRequest(request);
    Cart cart = userId == null
        ? findOrCreateGuest(guestCartId)
        : cartRepository.findByUserId(userId).orElseGet(() -> cartRepository.save(Cart.member(userId)));
    addValidatedItem(cart, request.offerId(), request.quantity());
    if (cart.isGuest()) {
      cart.renewGuest(clock);
    }
    cartRepository.save(cart);
    return cart.getId();
  }

  @Transactional
  public CartResponse getCart(UUID userId) {
    if (userId == null) {
      throw new CartException(CartErrorCode.CART_NOT_FOUND);
    }
    Cart cart = cartRepository.findByUserId(userId)
        .orElseGet(() -> cartRepository.save(Cart.member(userId)));
    return toResponse(cart);
  }

  @Transactional
  public void changeQuantity(UUID userId, UUID cartItemId, ChangeCartItemQuantityRequest request) {
    if (request == null || request.quantity() == null || request.quantity() < 0) {
      throw new CartException(CartErrorCode.CART_INVALID_INPUT);
    }
    Cart cart = requireMemberCart(userId);
    CartItem item = findOwnedItem(cart, cartItemId);
    if (request.quantity() == 0) {
      cart.removeItem(item.getId());
      return;
    }
    OfferQueryPort.OfferSnapshot offer = requirePurchasableOffer(item.getOfferId());
    validateOfferQuantity(offer, request.quantity());
    validateTotalQuantity(cart, item, request.quantity());
    item.changeQuantity(request.quantity());
  }

  @Transactional
  public void deleteItem(UUID userId, UUID cartItemId) {
    Cart cart = requireMemberCart(userId);
    findOwnedItem(cart, cartItemId);
    cart.removeItem(cartItemId);
  }

  @Transactional
  public void clear(UUID userId, UUID guestCartId) {
    if (userId != null) {
      cartRepository.findByUserId(userId).ifPresent(cart -> {
        cart.clearItems();
        cartRepository.save(cart);
      });
      return;
    }
    if (guestCartId != null) {
      cartRepository.findGuestById(guestCartId).ifPresent(cartRepository::delete);
    }
  }

  @Transactional
  public CartMergeResult mergeGuestCart(UUID userId, UUID guestCartId) {
    if (userId == null || guestCartId == null) {
      return new CartMergeResult(CartMergeResult.Status.CONFLICT, null, List.of(), List.of());
    }
    Cart guest = cartRepository.findGuestById(guestCartId).orElse(null);
    if (guest == null) {
      return new CartMergeResult(CartMergeResult.Status.MERGED, userId, List.of(), List.of());
    }
    if (guest.isExpired(Instant.now(clock))) {
      cartRepository.delete(guest);
      return new CartMergeResult(CartMergeResult.Status.MERGED, userId, List.of(), List.of());
    }
    Cart member = cartRepository.findByUserId(userId).orElse(null);
    List<UUID> conflicts = validateMerge(member, guest);
    if (!conflicts.isEmpty()) {
      return new CartMergeResult(CartMergeResult.Status.CONFLICT,
          member == null ? null : member.getId(), List.of(), conflicts);
    }
    if (member == null) {
      member = Cart.member(userId);
      cartRepository.save(member);
    }
    List<UUID> mergedIds = new ArrayList<>();
    for (CartItem guestItem : guest.getItems()) {
      CartItem current = member.findItem(guestItem.getOfferId());
      if (current == null) {
        current = member.addItem(guestItem.getOfferId(), guestItem.getQuantity());
      } else {
        current.changeQuantity(current.getQuantity() + guestItem.getQuantity());
      }
      mergedIds.add(current.getId());
    }
    cartRepository.save(member);
    cartRepository.delete(guest);
    return new CartMergeResult(CartMergeResult.Status.MERGED, member.getId(), mergedIds, List.of());
  }

  @Transactional
  public int cleanupAfterPayment(Collection<UUID> cartItemIds) {
    if (cartItemIds == null || cartItemIds.isEmpty()) {
      return 0;
    }
    return cartRepository.deleteItemsByIds(cartItemIds);
  }

  @Transactional
  public int cleanupExpiredGuests() {
    return cartRepository.deleteExpiredGuests(Instant.now(clock));
  }

  private Cart findOrCreateGuest(UUID guestCartId) {
    if (guestCartId != null) {
      Cart cart = cartRepository.findGuestById(guestCartId).orElse(null);
      if (cart != null && !cart.isExpired(Instant.now(clock))) {
        return cart;
      }
      if (cart != null) {
        cartRepository.delete(cart);
      }
    }
    return cartRepository.save(Cart.guest(UUID.randomUUID(), clock));
  }

  private Cart requireMemberCart(UUID userId) {
    if (userId == null) {
      throw new CartException(CartErrorCode.CART_NOT_FOUND);
    }
    return cartRepository.findByUserId(userId)
        .orElseThrow(() -> new CartException(CartErrorCode.CART_NOT_FOUND));
  }

  private CartItem findOwnedItem(Cart cart, UUID cartItemId) {
    return cart.getItems().stream().filter(item -> item.getId().equals(cartItemId)).findFirst()
        .orElseThrow(() -> new CartException(CartErrorCode.CART_NOT_FOUND));
  }

  private void addValidatedItem(Cart cart, UUID offerId, int quantity) {
    OfferQueryPort.OfferSnapshot offer = requirePurchasableOffer(offerId);
    CartItem existing = cart.findItem(offerId);
    int resultingQuantity = quantity + (existing == null ? 0 : existing.getQuantity());
    validateOfferQuantity(offer, resultingQuantity);
    if (existing == null && cart.getItems().size() >= Cart.MAX_ITEM_TYPES) {
      throw new CartException(CartErrorCode.CART_ITEM_LIMIT,
          AmaazonExceptionContext.logDetails(Map.of("itemCount", cart.getItems().size())));
    }
    if (existing == null) {
      if (cart.totalQuantity() + quantity > Cart.MAX_TOTAL_QUANTITY) {
        throw new CartException(CartErrorCode.CART_QUANTITY_LIMIT);
      }
      cart.addItem(offerId, quantity);
    } else {
      validateTotalQuantity(cart, existing, resultingQuantity);
      existing.changeQuantity(resultingQuantity);
    }
  }

  private List<UUID> validateMerge(Cart member, Cart guest) {
    List<UUID> conflicts = new ArrayList<>();
    int itemTypes = member == null ? 0 : member.getItems().size();
    int totalQuantity = member == null ? 0 : member.totalQuantity();
    for (CartItem guestItem : guest.getItems()) {
      OfferQueryPort.OfferSnapshot offer = offerQueryPort.findById(guestItem.getOfferId())
          .orElseThrow(() -> new OfferNotFoundException(guestItem.getOfferId()));
      if (!offer.isPurchasable() || offer.isOutOfStock()) {
        throw new CartException(CartErrorCode.CART_OFFER_UNAVAILABLE,
            new AmaazonExceptionContext(Map.of("offerId", guestItem.getOfferId()), Map.of(), null));
      }
      CartItem existing = member == null ? null : member.findItem(guestItem.getOfferId());
      if (guestItem.getQuantity() + (existing == null ? 0 : existing.getQuantity())
          > offer.maxPurchaseQuantity()) {
        conflicts.add(guestItem.getOfferId());
        continue;
      }
      if (existing == null) {
        itemTypes++;
      }
      totalQuantity += guestItem.getQuantity();
      if (itemTypes > Cart.MAX_ITEM_TYPES || totalQuantity > Cart.MAX_TOTAL_QUANTITY) {
        conflicts.add(guestItem.getOfferId());
      }
    }
    return conflicts;
  }

  private OfferQueryPort.OfferSnapshot requirePurchasableOffer(UUID offerId) {
    OfferQueryPort.OfferSnapshot offer = offerQueryPort.findById(offerId)
        .orElseThrow(() -> new OfferNotFoundException(offerId));
    if (!offer.isPurchasable() || offer.isOutOfStock()) {
      throw new CartException(CartErrorCode.CART_OFFER_UNAVAILABLE,
          new AmaazonExceptionContext(Map.of("offerId", offerId), Map.of(), null));
    }
    return offer;
  }

  private void validateOfferQuantity(OfferQueryPort.OfferSnapshot offer, int quantity) {
    if (quantity < 1) {
      throw new CartException(CartErrorCode.CART_INVALID_INPUT);
    }
    if (quantity > offer.maxPurchaseQuantity()) {
      throw new CartException(CartErrorCode.CART_OFFER_LIMIT,
          new AmaazonExceptionContext(
              Map.of("offerId", offer.offerId(), "maxPurchaseQuantity", offer.maxPurchaseQuantity()),
              Map.of(), null));
    }
  }

  private void validateTotalQuantity(Cart cart, CartItem item, int quantity) {
    int result = cart.totalQuantity() - item.getQuantity() + quantity;
    if (result > Cart.MAX_TOTAL_QUANTITY) {
      throw new CartException(CartErrorCode.CART_QUANTITY_LIMIT);
    }
  }

  private void validateAddRequest(AddCartItemRequest request) {
    if (request == null || request.offerId() == null || request.quantity() == null
        || request.quantity() < 1) {
      throw new CartException(CartErrorCode.CART_INVALID_INPUT);
    }
  }

  private CartResponse toResponse(Cart cart) {
    List<CartResponse.CartItemResponse> items = new ArrayList<>();
    BigDecimal total = BigDecimal.ZERO;
    String currency = "KRW";
    for (CartItem item : cart.getItems()) {
      OfferQueryPort.OfferSnapshot offer = offerQueryPort.findById(item.getOfferId()).orElse(null);
      boolean unavailable = offer == null || !offer.isPurchasable();
      boolean outOfStock = offer != null && offer.isOutOfStock();
      BigDecimal unitPrice = offer == null || offer.currentPrice() == null
          ? BigDecimal.ZERO : offer.currentPrice();
      String itemCurrency = offer == null || offer.currency() == null ? "KRW" : offer.currency();
      if (offer != null && offer.currency() != null) {
        currency = offer.currency();
      }
      BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
      total = total.add(subtotal);
      items.add(new CartResponse.CartItemResponse(item.getId(), item.getOfferId(),
          offer == null ? null : offer.name(), offer == null ? null : offer.thumbnailUrl(),
          item.getQuantity(), new CartResponse.Money(unitPrice, itemCurrency),
          new CartResponse.Money(subtotal, itemCurrency), outOfStock, unavailable));
    }
    CartResponse.Money money = new CartResponse.Money(total, currency);
    return new CartResponse(cart.getId(), items, items.size(), cart.totalQuantity(), money, money);
  }
}
