package io.github.metdaisy.amaazon.catalog.domain.entity;

import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogStatus;
import io.github.metdaisy.amaazon.catalog.domain.entity.util.AttributesUpdater;
import io.github.metdaisy.amaazon.catalog.domain.exception.ProductVariantErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.ProductVariantException;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import io.github.metdaisy.amaazon.common.jpa.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "product_variants")
@SQLDelete(sql = "UPDATE product_variants "
    + "SET publication_status = 'ARCHIVED', archived_at = now(), updated_at = now() "
    + "WHERE id = ?")
public class ProductVariant extends MutableEntity {

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "catalog_product_id", nullable = false)
  private CatalogProduct catalogProduct;

  @NotBlank
  @Size(max = 255)
  @Column(name = "display_name", nullable = false)
  private String displayName;

  @NotNull
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "attributes", nullable = false)
  private Map<String, Object> attributes = new HashMap<>();

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "publication_status", nullable = false, length = 20)
  private CatalogStatus publicationStatus;

  @Column(name = "archived_at")
  private Instant archivedAt;

  private ProductVariant(CatalogProduct catalogProduct, String displayName,
      Map<String, Object> attributes) {
    if (attributes == null) {
      throw new ProductVariantException(ProductVariantErrorCode.VARIANT_INVALID);
    }
    this.catalogProduct = catalogProduct;
    this.displayName = displayName;
    this.attributes = new LinkedHashMap<>(attributes);
    this.publicationStatus = CatalogStatus.ACTIVE;
  }

  public static ProductVariant of(CatalogProduct catalogProduct, String displayName,
      Map<String, Object> attributes) {
    return new ProductVariant(catalogProduct, displayName, attributes);
  }

  public void validateActive() {
    if (publicationStatus == CatalogStatus.ARCHIVED) {
      throw new ProductVariantException(ProductVariantErrorCode.VARIANT_ARCHIVED,
          AmaazonExceptionContext.logDetails(Map.of("variantId", getId())));
    }
  }

  public boolean isActive() {
    return publicationStatus == CatalogStatus.ACTIVE;
  }

  public void archive() {
    if (!isActive()) {
      throw new ProductVariantException(ProductVariantErrorCode.VARIANT_ALREADY_ARCHIVED,
          AmaazonExceptionContext.logDetails(Map.of("variantId", getId())));
    }
    publicationStatus = CatalogStatus.ARCHIVED;
    archivedAt = Instant.now();
    setUpdatedAt(archivedAt);
  }

  public void setAttributes(Map<String, Object> attributes) {
    this.attributes = AttributesUpdater.update(this.attributes, attributes);
  }
}
