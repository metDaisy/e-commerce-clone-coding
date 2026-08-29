package io.github.metdaisy.amaazon.catalog.infra.repository;

import static io.github.metdaisy.amaazon.catalog.domain.entity.QCatalogProduct.catalogProduct;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.github.metdaisy.amaazon.catalog.domain.entity.QCatalogProductTag;
import io.github.metdaisy.amaazon.catalog.domain.entity.QProductVariant;
import io.github.metdaisy.amaazon.catalog.domain.entity.QTag;
import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.ArchiveStatus;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogIdentifierType;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogProductSort;
import io.github.metdaisy.amaazon.common.dto.PageQuery;
import io.github.metdaisy.amaazon.common.dto.PageResult;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CatalogProductQueryRepositoryImpl implements CatalogProductQueryRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public PageResult<CatalogProduct> findPage(Set<UUID> categoryIds, String keyword, String tag,
      CatalogProductSort sort, ArchiveStatus catalogPublicationStatus,
      ArchiveStatus variantPublicationStatus, PageQuery pageQuery) {
    BooleanBuilder predicate = createPredicate(categoryIds, keyword, tag,
        catalogPublicationStatus, variantPublicationStatus);
    List<CatalogProduct> content = queryFactory.selectFrom(catalogProduct)
        .where(predicate)
        .orderBy(orderBy(sort))
        .offset((long) pageQuery.page() * pageQuery.size())
        .limit(pageQuery.size())
        .fetch();

    long totalElements = content.size() < pageQuery.size() && pageQuery.page() == 0
        ? content.size()
        : count(predicate);
    int totalPages = (int) ((totalElements + pageQuery.size() - 1) / pageQuery.size());
    return new PageResult<>(content, pageQuery.page(), pageQuery.size(), totalElements,
        totalPages);
  }

  @Override
  public boolean existsIdentifier(UUID id, CatalogIdentifierType type, String value) {
    BooleanBuilder predicate = new BooleanBuilder(identifierPath(type).eq(value));
    if (id != null) {
      predicate.and(catalogProduct.id.ne(id));
    }
    return queryFactory.selectOne()
        .from(catalogProduct)
        .where(predicate)
        .fetchFirst() != null;
  }

  private BooleanBuilder createPredicate(Set<UUID> categoryIds, String keyword, String tag,
      ArchiveStatus catalogPublicationStatus, ArchiveStatus variantPublicationStatus) {
    BooleanBuilder predicate = new BooleanBuilder(
        catalogProduct.publicationStatus.eq(catalogPublicationStatus));
    if (categoryIds != null && !categoryIds.isEmpty()) {
      predicate.and(catalogProduct.category.id.in(categoryIds));
    }
    if (keyword != null && !keyword.isBlank()) {
      predicate.and(keywordPredicate(keyword, variantPublicationStatus));
    }
    if (tag != null && !tag.isBlank()) {
      predicate.and(tagPredicate(tag));
    }
    return predicate;
  }

  private Predicate keywordPredicate(String keyword,
      ArchiveStatus variantPublicationStatus) {
    String normalizedKeyword = keyword.trim();
    QProductVariant variant = new QProductVariant("variant");
    return catalogProduct.name.containsIgnoreCase(normalizedKeyword)
        .or(catalogProduct.description.containsIgnoreCase(normalizedKeyword))
        .or(catalogProduct.brand.containsIgnoreCase(normalizedKeyword))
        .or(JPAExpressions.selectOne()
            .from(variant)
            .where(variant.catalogProduct.eq(catalogProduct),
                variant.publicationStatus.eq(variantPublicationStatus),
                variant.displayName.containsIgnoreCase(normalizedKeyword))
            .exists());
  }

  private Predicate tagPredicate(String tagName) {
    String normalizedTag = tagName.trim().toLowerCase(Locale.ROOT);
    QCatalogProductTag productTag = new QCatalogProductTag("productTag");
    QTag tag = new QTag("tag");
    return JPAExpressions.selectOne()
        .from(productTag)
        .join(productTag.tag, tag)
        .where(productTag.catalogProduct.eq(catalogProduct),
            tag.name.equalsIgnoreCase(normalizedTag))
        .exists();
  }

  private long count(BooleanBuilder predicate) {
    Long count = queryFactory.select(catalogProduct.id.count())
        .from(catalogProduct)
        .where(predicate)
        .fetchOne();
    return count == null ? 0 : count;
  }

  private OrderSpecifier<?>[] orderBy(CatalogProductSort sort) {
    return switch (sort) {
      case NAME_ASC -> new OrderSpecifier<?>[]{catalogProduct.name.asc(), catalogProduct.id.asc()};
      case NAME_DESC ->
          new OrderSpecifier<?>[]{catalogProduct.name.desc(), catalogProduct.id.desc()};
      case LATEST ->
          new OrderSpecifier<?>[]{catalogProduct.createdAt.desc(), catalogProduct.id.desc()};
    };
  }

  private StringPath identifierPath(CatalogIdentifierType type) {
    Map<CatalogIdentifierType, StringPath> paths = Map.of(
        CatalogIdentifierType.ASIN, catalogProduct.asin,
        CatalogIdentifierType.GTIN, catalogProduct.gtin,
        CatalogIdentifierType.UPC, catalogProduct.upc,
        CatalogIdentifierType.EAN, catalogProduct.ean,
        CatalogIdentifierType.ISBN, catalogProduct.isbn);
    return paths.get(type);
  }
}
