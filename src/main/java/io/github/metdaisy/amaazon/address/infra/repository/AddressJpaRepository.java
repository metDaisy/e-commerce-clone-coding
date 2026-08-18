package io.github.metdaisy.amaazon.address.infra.repository;

import io.github.metdaisy.amaazon.address.domain.entity.Address;
import io.github.metdaisy.amaazon.address.domain.repository.AddressRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AddressJpaRepository extends JpaRepository<Address, UUID>, AddressRepository {

  @Override
  Optional<Address> findById(UUID id);

  @Override
  @Query("select a from Address a where a.userId = :userId "
      + "order by a.isPrimary desc, a.createdAt desc, a.id desc")
  List<Address> findByUserId(@Param("userId") UUID userId);

  @Override
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update Address a set a.isPrimary = false where a.userId = :userId "
      + "and a.isPrimary = true")
  int clearPrimaryByUserId(@Param("userId") UUID userId);

  @Override
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update Address a set a.isPrimary = true "
      + "where a.id = :id and a.userId = :userId")
  int makePrimaryByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
