package io.github.metdaisy.amaazon.product.application.service;

import io.github.metdaisy.amaazon.product.application.dto.ProductCreateRequest;
import io.github.metdaisy.amaazon.product.application.dto.ProductResponse;
import io.github.metdaisy.amaazon.product.application.mapper.ProductMapper;
import io.github.metdaisy.amaazon.product.domain.entity.Category;
import io.github.metdaisy.amaazon.product.domain.entity.Product;
import io.github.metdaisy.amaazon.product.domain.entity.ProductTag;
import io.github.metdaisy.amaazon.product.domain.entity.Tag;
import io.github.metdaisy.amaazon.product.domain.exception.ProductErrorCode;
import io.github.metdaisy.amaazon.product.domain.exception.ProductException;
import io.github.metdaisy.amaazon.product.domain.repository.CategoryRepository;
import io.github.metdaisy.amaazon.product.domain.repository.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {

  private final TagService tagService;
  private final ProductRepository repository;
  private final CategoryRepository categoryRepository;
  private final ProductMapper mapper;

  @Transactional
  public UUID create(UUID managerId, ProductCreateRequest request) {
    Category category = findCategory(request.category());
    List<Tag> tags = tagService.findAndCreate(request.tags());
    Product product = Product.create(category, managerId, request.name(),
        request.description(), request.price());
    Set<ProductTag> productTags = createProductTags(tags, product);
    product.setProductTags(productTags);
    repository.save(product);
    return product.getId();
  }

  public ProductResponse find(UUID id) {
    return repository.findById(id).map(mapper::toDto).orElseThrow(
        () -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND, Map.of("productId", id)));
  }

  private Set<ProductTag> createProductTags(List<Tag> tags, Product product) {
    return tags.stream()
        .map(tag -> ProductTag.of(product, tag))
        .collect(Collectors.toSet());
  }

  private Category findCategory(String categoryName) {
    return categoryRepository.findByName(categoryName)
        .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_CATEGORY_NOT_FOUND,
            Map.of("category", categoryName)));
  }

}
