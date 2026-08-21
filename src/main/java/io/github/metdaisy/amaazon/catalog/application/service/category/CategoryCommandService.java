package io.github.metdaisy.amaazon.catalog.application.service.category;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CategoryCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CategoryUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CategoryResponse;
import io.github.metdaisy.amaazon.catalog.application.mapper.CategoryMapper;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.exception.CategoryErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CategoryException;
import io.github.metdaisy.amaazon.catalog.domain.repository.CategoryRepository;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryCommandService {

  private final CategoryRepository repository;
  private final CategoryMapper mapper;

  @Transactional
  @CacheEvict(cacheNames = "categories", allEntries = true)
  public CategoryResponse create(CategoryCreateRequest request) {
    Category parent = findParent(request.parentId());
    validateUniqueName(request.name(), null);
    validateDepth(parent == null ? 1 : parent.getDepth() + 1);
    Category category = Category.of(request.name(), parent);
    return mapper.toDto(repository.save(category));
  }

  @Transactional
  @CacheEvict(cacheNames = "categories", allEntries = true)
  public CategoryResponse update(UUID id, CategoryUpdateRequest request) {
    Category category = findById(id);
    Category parent = findParent(request.parentId());
    validateUniqueName(request.name(), id);
    updateHierarchy(category, parent);
    category.rename(request.name());
    return mapper.toDto(category);
  }

  private Category findById(UUID id) {
    return repository.findById(id).orElseThrow(() -> new CategoryException(
        CategoryErrorCode.CATEGORY_NOT_FOUND,
        AmaazonExceptionContext.logDetails(Map.of("categoryId", id))));
  }

  private Category findParent(UUID parentId) {
    return parentId == null ? null : findById(parentId);
  }

  private void validateDepth(int depth) {
    if (depth > 3) {
      throw new CategoryException(CategoryErrorCode.CATEGORY_DEPTH_EXCEEDED,
          AmaazonExceptionContext.logDetails(Map.of("depth", depth)));
    }
  }

  private void validateUniqueName(String name, UUID excludedId) {
    boolean duplicate = excludedId == null
        ? repository.existsByName(name)
        : repository.existsByNameAndIdNot(name, excludedId);
    if (duplicate) {
      throw new CategoryException(CategoryErrorCode.CATEGORY_NAME_DUPLICATE,
          AmaazonExceptionContext.logDetails(Map.of("name", name)));
    }
  }

  private void updateHierarchy(Category category, Category newParent) {
    int newDepth = newParent == null ? 1 : newParent.getDepth() + 1;
    List<Category> descendants = repository.findAll().stream()
        .filter(candidate -> isDescendant(candidate, category))
        .toList();
    int maxRelativeDepth = descendants.stream()
        .mapToInt(candidate -> relativeDepth(candidate, category))
        .max()
        .orElse(0);
    validateDepth(newDepth + maxRelativeDepth);

    int depthDelta = newDepth - category.getDepth();
    category.moveTo(newParent);
    descendants.forEach(descendant -> descendant.updateDepth(descendant.getDepth() + depthDelta));
  }

  private boolean isDescendant(Category candidate, Category ancestor) {
    Category current = candidate.getParent();
    while (current != null) {
      if (Objects.equals(current.getId(), ancestor.getId())) {
        return true;
      }
      current = current.getParent();
    }
    return false;
  }

  private int relativeDepth(Category descendant, Category ancestor) {
    int depth = 0;
    Category current = descendant;
    while (current != null && !Objects.equals(current.getId(), ancestor.getId())) {
      depth++;
      current = current.getParent();
    }
    return depth;
  }
}
