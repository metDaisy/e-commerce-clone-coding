package io.github.metdaisy.amaazon.address.infra.repository;

import io.github.metdaisy.amaazon.address.domain.entity.Address;
import io.github.metdaisy.amaazon.address.domain.repository.AddressRepository;
import io.github.metdaisy.amaazon.common.dto.PageQuery;
import io.github.metdaisy.amaazon.common.dto.PageResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AddressJpaRepository extends JpaRepository<Address, UUID>, AddressRepository,
    AddressQuerydslRepository {

  @Override
  Optional<Address> findById(UUID id);

  Sort ADDRESS_SORT = Sort.by(
      Sort.Order.desc("isPrimary"),
      Sort.Order.desc("lastUsedAt").nullsLast(),
      Sort.Order.desc("createdAt"),
      Sort.Order.desc("id"));

  @Override
  default PageResult<Address> findPageByUserId(UUID userId, PageQuery pageQuery) {
    Page<Address> page = this.findByUserId(userId,
        PageRequest.of(pageQuery.page(), pageQuery.size(), ADDRESS_SORT));
    return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(),
        page.getTotalElements(), page.getTotalPages());
  }

  @Override
  boolean existsByIdAndUserId(UUID id, UUID userId);

  @Query("select a from Address a "
      + "where a.userId = :userId "
      + "order by a.isPrimary desc, a.lastUsedAt desc nulls last, "
      + "a.createdAt desc, a.id desc")
  List<Address> findByUserId(UUID userId);

  @Query("select a from Address a where a.userId = :userId")
  Page<Address> findByUserId(UUID userId, Pageable pageable);

  @Override
  @Query("select case when exists (select 1 from Address a "
      + "where a.userId = :userId "
      + "and a.postalCode = :postalCode "
      + "and a.addressLine = :addressLine) then true else false end")
  boolean existsByUserIdAndPostalCodeAndAddressLine(UUID userId, String postalCode,
      String addressLine);

  @Override
  @Query("select case when exists (select 1 from Address a "
      + "where a.userId = :userId "
      + "and a.postalCode = :postalCode "
      + "and a.addressLine = :addressLine "
      + "and a.id <> :id) then true else false end")
  boolean existsByUserIdAndPostalCodeAndAddressLineAndIdNot(UUID userId,
      String postalCode,
      String addressLine,
      UUID id);

  @Override
  @Modifying
  @Query("update Address a set a.isPrimary = false "
      + "where a.userId = :userId and a.isPrimary = true")
  int clearPrimaryByUserId(UUID userId);

}
