package io.github.metdaisy.amaazon.address.application.service;

import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import io.github.metdaisy.amaazon.address.application.dto.request.AddressCreateRequest;
import io.github.metdaisy.amaazon.address.application.dto.request.AddressUpdateRequest;
import io.github.metdaisy.amaazon.address.application.dto.response.AddressResponse;
import io.github.metdaisy.amaazon.address.application.mapper.AddressMapper;
import io.github.metdaisy.amaazon.address.domain.entity.Address;
import io.github.metdaisy.amaazon.address.domain.exception.AddressErrorCode;
import io.github.metdaisy.amaazon.address.domain.exception.AddressException;
import io.github.metdaisy.amaazon.address.domain.repository.AddressRepository;
import io.github.metdaisy.amaazon.user.application.port.in.UserQueryApi;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AddressService {

  private static final int MAX_ADDRESS_COUNT = 5;
  private final AddressRepository repository;
  private final AddressMapper addressMapper;
  private final UserQueryApi userQueryApi;

  public List<AddressResponse> findAll(UUID userId) {
    validateEnabledUser(userId);
    return repository.findByUserId(userId).stream().map(addressMapper::toDto).toList();
  }

  @Transactional
  public AddressResponse create(UUID userId, AddressCreateRequest request) {
    validateEnabledUser(userId);
    long addressCount = repository.countByUserId(userId);
    if (addressCount >= MAX_ADDRESS_COUNT) {
      throw new AddressException(AddressErrorCode.ADDRESS_LIMIT_EXCEEDED,
          AmaazonExceptionContext.logDetails(Map.of(
              "userId", userId,
              "addressCount", addressCount,
              "max", MAX_ADDRESS_COUNT)));
    }

    if (request.isPrimary()) {
      repository.clearPrimaryByUserId(userId);
    }
    Address address = Address.create(userId, request.recipientName(), request.recipientPhone(),
        request.postalCode(), request.addressLine(), request.isPrimary());
    return addressMapper.toDto(repository.save(address));
  }

  @Transactional
  public AddressResponse update(UUID userId, UUID addressId, AddressUpdateRequest request) {
    validateEnabledUser(userId);
    Address address = findOwnedAddress(userId, addressId);
    address.updateRecipientName(request.recipientName());
    address.updateRecipientPhone(request.recipientPhone());
    address.updatePostalCode(request.postalCode());
    address.updateAddressLine(request.addressLine());
    return addressMapper.toDto(address);
  }

  @Transactional
  public void delete(UUID userId, UUID addressId) {
    validateEnabledUser(userId);
    Address address = findOwnedAddress(userId, addressId);
    boolean wasPrimary = address.isPrimary();
    repository.delete(address);

    if (wasPrimary) {
      repository.findByUserId(userId).stream()
          .findFirst()
          .ifPresent(Address::makePrimary);
    }
  }

  @Transactional
  public AddressResponse makePrimary(UUID userId, UUID addressId) {
    validateEnabledUser(userId);
    validateAddressOwnership(userId, addressId);
    repository.clearPrimaryByUserId(userId);
    int updatedCount = repository.makePrimaryByIdAndUserId(addressId, userId);
    if (updatedCount == 0) {
      throw new AddressException(AddressErrorCode.ADDRESS_NOT_FOUND,
          AmaazonExceptionContext.logDetails(Map.of(
              "userId", userId,
              "addressId", addressId)));
    }
    Address address = repository.findById(addressId)
        .orElseThrow(() -> new AddressException(AddressErrorCode.ADDRESS_NOT_FOUND,
            AmaazonExceptionContext.logDetails(Map.of(
                "userId", userId,
                "addressId", addressId))));
    return addressMapper.toDto(address);
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

  private void validateEnabledUser(UUID userId) {
    userQueryApi.requireEnabled(userId);
  }

}
