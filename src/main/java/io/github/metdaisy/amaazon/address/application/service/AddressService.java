package io.github.metdaisy.amaazon.address.application.service;

import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import io.github.metdaisy.amaazon.address.application.dto.request.AddressCreateRequest;
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

  private void validateEnabledUser(UUID userId) {
    userQueryApi.requireEnabled(userId);
  }

}
