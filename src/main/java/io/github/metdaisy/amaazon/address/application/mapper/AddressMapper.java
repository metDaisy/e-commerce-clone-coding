package io.github.metdaisy.amaazon.address.application.mapper;

import io.github.metdaisy.amaazon.address.application.dto.request.AddressCreateRequest;
import io.github.metdaisy.amaazon.address.application.dto.request.AddressUpdateRequest;
import io.github.metdaisy.amaazon.common.mapper.GlobalMapperConfig;
import io.github.metdaisy.amaazon.address.application.dto.response.AddressResponse;
import io.github.metdaisy.amaazon.address.domain.entity.Address;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class)
public interface AddressMapper {

  @Mapping(target = "isPrimary", source = "primary")
  AddressResponse toDto(Address address);

  Address toEntity(UUID userId, AddressCreateRequest request);

  void update(@MappingTarget Address address, AddressUpdateRequest request);
}
