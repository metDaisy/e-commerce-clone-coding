package io.github.metdaisy.amaazon.address.application.mapper;

import io.github.metdaisy.amaazon.common.mapper.GlobalMapperConfig;
import io.github.metdaisy.amaazon.address.application.dto.response.AddressResponse;
import io.github.metdaisy.amaazon.address.domain.entity.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface AddressMapper {

  @Mapping(target = "isPrimary", source = "primary")
  AddressResponse toDto(Address address);
}
