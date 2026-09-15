package io.github.metdaisy.amaazon.catalog.application.service.variant;

import io.github.metdaisy.amaazon.catalog.application.dto.response.ProductVariantDto;
import io.github.metdaisy.amaazon.catalog.application.mapper.ProductVariantMapper;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.ArchiveStatus;
import io.github.metdaisy.amaazon.catalog.domain.repository.ProductVariantRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductVariantQueryService {

  private final ProductVariantRepository repository;
  private final ProductVariantMapper mapper;

  public List<ProductVariantDto> findByCatalogProductId(UUID catalogProductId,
      ArchiveStatus publicationStatus) {
    return findByCatalogProductIds(List.of(catalogProductId), publicationStatus)
        .getOrDefault(catalogProductId, Collections.emptyList());
  }

  public Map<UUID, List<ProductVariantDto>> findByCatalogProductIds(List<UUID> catalogProductIds,
      ArchiveStatus publicationStatus) {
    if (catalogProductIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return repository.findByCatalogProductIdsAndPublicationStatus(catalogProductIds,
            publicationStatus)
        .stream()
        .map(mapper::toDto)
        .collect(Collectors.groupingBy(ProductVariantDto::catalogProductId,
            Collectors.toList()));
  }
}
