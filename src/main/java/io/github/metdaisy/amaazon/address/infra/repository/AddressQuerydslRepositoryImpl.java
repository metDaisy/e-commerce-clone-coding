package io.github.metdaisy.amaazon.address.infra.repository;

import static io.github.metdaisy.amaazon.address.domain.entity.QAddress.address;

import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.github.metdaisy.amaazon.address.domain.entity.Address;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class AddressQuerydslRepositoryImpl implements AddressQuerydslRepository {

  private final JPAQueryFactory queryFactory;
  private final EntityManager em;

  @Override
  @Transactional
  public void deleteAndUpdatePrimary(UUID userId, UUID addressId) {
    lockAddressesByUserId(userId);
    List<Address> addresses = queryFactory.selectFrom(address)
        .where(address.userId.eq(userId))
        .orderBy(
            new CaseBuilder().when(address.id.eq(addressId)).then(0).otherwise(1).asc(),
            address.isPrimary.desc(),
            address.lastUsedAt.desc().nullsLast(),
            address.createdAt.desc(),
            address.id.desc())
        .limit(2)
        .fetch();

    Address target = addresses.get(0);
    boolean wasPrimary = target.isPrimary();
    em.remove(target);
    em.flush();
    if (wasPrimary && addresses.size() == 2) {
      addresses.get(1).setPrimary(true);
    }
  }

  @Override
  @Transactional
  public Address makePrimary(UUID userId, UUID addressId) {
    lockAddressesByUserId(userId);

    List<Address> addresses = queryFactory.selectFrom(address)
        .where(address.userId.eq(userId)
            .and(address.id.eq(addressId).or(address.isPrimary.isTrue())))
        .orderBy(new CaseBuilder().when(address.id.eq(addressId)).then(0).otherwise(1).asc())
        .limit(2)
        .fetch();
    if (addresses.size() == 2) {
      addresses.get(1).setPrimary(false);
      em.flush();
    }
    Address target = addresses.get(0);
    target.setPrimary(true);
    return target;
  }

  private void lockAddressesByUserId(UUID userId) {
    em.createNativeQuery("select 1 from addresses where user_id = :userId for update")
        .setParameter("userId", userId)
        .getResultList();
  }
}
