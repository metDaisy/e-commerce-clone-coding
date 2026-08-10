package io.github.metdaisy.amaazon.catalog.application.service;

import io.github.metdaisy.amaazon.catalog.application.dto.response.CategoryResponse;
import io.github.metdaisy.amaazon.catalog.application.mapper.CategoryMapper;
import io.github.metdaisy.amaazon.catalog.domain.repository.CategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryService {

  private final CategoryRepository repository;
  private final CategoryMapper mapper;

  public List<CategoryResponse> findAll() {
    return mapper.toDto(repository.findAll());
  }
}
