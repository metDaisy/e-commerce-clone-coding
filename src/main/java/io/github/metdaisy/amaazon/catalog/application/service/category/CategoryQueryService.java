package io.github.metdaisy.amaazon.catalog.application.service.category;

import io.github.metdaisy.amaazon.catalog.application.dto.response.CategoryDto;
import io.github.metdaisy.amaazon.catalog.application.mapper.CategoryMapper;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.exception.CategoryErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CategoryException;
import io.github.metdaisy.amaazon.catalog.domain.repository.CategoryRepository;
import io.github.metdaisy.amaazon.global.infra.cache.constant.CacheConstants;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

  @Cacheable(cacheNames = CacheConstants.CATEGORIES)
  public List<CategoryDto> findAll() {
    return mapper.toDto(repository.findAll());
  }

  public Category getProxy(UUID id) {
    if (!repository.existsById(id)) {
      throw new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND,
          AmaazonExceptionContext.logDetails(Map.of("categoryId", id)));
    }
    return repository.getReferenceById(id);
  }

  public Set<UUID> findSelfAndDescendantIds(UUID categoryId) {
    for (CategoryDto category : findAll()) {
      Optional<CategoryDto> found = findCategory(category, categoryId);
      if (found.isPresent()) {
        return collectIds(found.get());
      }
    }
    throw new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND,
        AmaazonExceptionContext.logDetails(Map.of("categoryId", categoryId)));
  }

  private Optional<CategoryDto> findCategory(CategoryDto category,
      UUID categoryId) {
    if (category.id().equals(categoryId)) {
      return Optional.of(category);
    }
    return category.children().stream()
        .map(child -> findCategory(child, categoryId))
        .filter(Optional::isPresent)
        .findFirst()
        .orElseGet(Optional::empty);
  }

  private Set<UUID> collectIds(CategoryDto category) {
    Set<UUID> ids = new LinkedHashSet<>();
    ids.add(category.id());
    category.children().forEach(child -> ids.addAll(collectIds(child)));
    return ids;
  }
}
