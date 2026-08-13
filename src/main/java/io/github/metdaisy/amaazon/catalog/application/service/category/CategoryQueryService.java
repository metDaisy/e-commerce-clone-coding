package io.github.metdaisy.amaazon.catalog.application.service.category;

import io.github.metdaisy.amaazon.catalog.application.dto.response.CategoryResponse;
import io.github.metdaisy.amaazon.catalog.application.mapper.CategoryMapper;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.catalog.domain.repository.CategoryRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryQueryService {

  private final CategoryRepository repository;
  private final CategoryMapper mapper;

  @Cacheable(cacheNames = "categories")
  public List<CategoryResponse> findAll() {
    return mapper.toDto(repository.findAll());
  }

  public Category getProxy(UUID id) {
    if (!repository.existsById(id)) {
      throw new CatalogProductException(CatalogProductErrorCode.CATEGORY_NOT_FOUND,
          Map.of("categoryId", id));
    }
    return repository.getReferenceById(id);
  }
}
