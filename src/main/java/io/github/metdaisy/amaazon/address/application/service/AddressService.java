package io.github.metdaisy.amaazon.address.application.service;

import io.github.metdaisy.amaazon.address.application.dto.request.AddressCreateRequest;
import io.github.metdaisy.amaazon.address.application.dto.request.AddressUpdateRequest;
import io.github.metdaisy.amaazon.address.application.dto.response.AddressResponse;
import io.github.metdaisy.amaazon.address.application.mapper.AddressMapper;
import io.github.metdaisy.amaazon.address.domain.entity.Address;
import io.github.metdaisy.amaazon.address.domain.exception.AddressErrorCode;
import io.github.metdaisy.amaazon.address.domain.exception.AddressException;
import io.github.metdaisy.amaazon.address.domain.repository.AddressRepository;
import io.github.metdaisy.amaazon.common.dto.PageQuery;
import io.github.metdaisy.amaazon.common.dto.PageResult;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AddressService {

  private final AddressRepository repository;
  private final AddressMapper mapper;

  public PageResult<AddressResponse> findAll(UUID userId, PageQuery pageQuery) {
    PageResult<Address> page = repository.findPageByUserId(userId, pageQuery);
    return new PageResult<>(page.content().stream().map(mapper::toDto).toList(),
        page.page(), page.size(), page.totalElements(), page.totalPages());
  }

  @Transactional
  public AddressResponse create(UUID userId, AddressCreateRequest request) {
    if (repository.existsByUserIdAndPostalCodeAndAddressLine(userId, request.postalCode(),
        request.addressLine())) {
      throw duplicateAddressException(userId, request.postalCode(), request.addressLine());
    }
    if (request.isPrimary()) {
      repository.clearPrimaryByUserId(userId);
    }
    Address address = mapper.toEntity(userId, request);
    repository.save(address);
    return mapper.toDto(address);
  }

  @Transactional
  public AddressResponse update(UUID userId, UUID addressId, AddressUpdateRequest request) {
    Address address = findOwnedAddress(userId, addressId);
    String postalCode = request.postalCode() == null
        ? address.getPostalCode() : request.postalCode();
    String addressLine = request.addressLine() == null
        ? address.getAddressLine() : request.addressLine();
    if (repository.existsByUserIdAndPostalCodeAndAddressLineAndIdNot(userId, postalCode,
        addressLine, addressId)) {
      throw duplicateAddressException(userId, postalCode, addressLine);
    }
    mapper.update(address, request);
    return mapper.toDto(address);
  }

  @Transactional
  public void delete(UUID userId, UUID addressId) {
    validateAddressOwnership(userId, addressId);
    repository.deleteAndUpdatePrimary(userId, addressId);
  }

  @Transactional
  public AddressResponse makePrimary(UUID userId, UUID addressId) {
    validateAddressOwnership(userId, addressId);
    Address target = repository.makePrimary(userId, addressId);
    return mapper.toDto(target);
  }

  private void validateAddressOwnership(UUID userId, UUID addressId) {
    if (!repository.existsById(addressId)) {
      throw new AddressException(AddressErrorCode.ADDRESS_NOT_FOUND,
          AmaazonExceptionContext.logDetails(Map.of(
              "userId", userId,
              "addressId", addressId)));
    }
    if (!repository.existsByIdAndUserId(addressId, userId)) {
      throw new AddressException(AddressErrorCode.ADDRESS_ACCESS_DENIED,
          AmaazonExceptionContext.logDetails(Map.of(
              "userId", userId,
              "addressId", addressId)));
    }
  }

  private Address findOwnedAddress(UUID userId, UUID addressId) {
    Address address = repository.findById(addressId)
        .orElseThrow(() -> new AddressException(AddressErrorCode.ADDRESS_NOT_FOUND,
            AmaazonExceptionContext.logDetails(Map.of(
                "userId", userId,
                "addressId", addressId))));
    if (!userId.equals(address.getUserId())) {
      throw new AddressException(AddressErrorCode.ADDRESS_ACCESS_DENIED,
          AmaazonExceptionContext.logDetails(Map.of(
              "userId", userId,
              "addressId", addressId,
              "ownerUserId", address.getUserId())));
    }
    return address;
  }

  private AddressException duplicateAddressException(UUID userId, String postalCode,
      String addressLine) {
    return new AddressException(AddressErrorCode.ADDRESS_DUPLICATED,
        AmaazonExceptionContext.logDetails(Map.of(
            "userId", userId,
            "postalCode", postalCode,
            "addressLine", addressLine)));
  }

}
