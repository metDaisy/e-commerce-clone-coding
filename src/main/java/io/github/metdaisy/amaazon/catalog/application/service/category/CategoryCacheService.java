package io.github.metdaisy.amaazon.catalog.application.service.category;

import io.github.metdaisy.amaazon.catalog.application.dto.response.CategoryDto;
import io.github.metdaisy.amaazon.catalog.application.mapper.CategoryMapper;
import io.github.metdaisy.amaazon.catalog.domain.repository.CategoryRepository;
import io.github.metdaisy.amaazon.global.infra.cache.constant.CacheConstants;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryCacheService {

  private final CategoryRepository repository;
  private final CategoryMapper mapper;

  @Cacheable(cacheNames = CacheConstants.CATEGORIES)
  public List<CategoryDto> findAll() {
    return mapper.toDto(repository.findAll());
  }
}
