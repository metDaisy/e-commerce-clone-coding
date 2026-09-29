package io.github.metdaisy.amaazon.cart.infra.repository;

import io.github.metdaisy.amaazon.cart.domain.entity.Cart;
import io.github.metdaisy.amaazon.cart.domain.entity.CartItem;
import io.github.metdaisy.amaazon.cart.domain.repository.CartRepository;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartJpaRepository extends JpaRepository<Cart, UUID>, CartRepository {

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select distinct c from Cart c left join fetch c.items where c.userId = :userId")
  Optional<Cart> findByUserId(@Param("userId") UUID userId);

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select distinct c from Cart c left join fetch c.items "
      + "where c.id = :cartId and c.userId is null")
  Optional<Cart> findGuestById(@Param("cartId") UUID cartId);

  @Override
  @Query("select i from CartItem i join fetch i.cart where i.id = :itemId")
  Optional<CartItem> findItemById(@Param("itemId") UUID itemId);

  @Override
  @Query("select c from Cart c where c.userId is null and c.expiresAt < :now")
  List<Cart> findExpiredGuests(@Param("now") Instant now);

  @Override
  @Modifying
  @Query("delete from CartItem i where i.id in :itemIds")
  int deleteItemsByIds(@Param("itemIds") Collection<UUID> itemIds);

  @Override
  @Modifying
  @Query("delete from Cart c where c.userId is null and c.expiresAt < :now")
  int deleteExpiredGuests(@Param("now") Instant now);
}
