package io.github.metdaisy.amaazon.catalog.domain.entity;

import io.github.metdaisy.amaazon.common.jpa.ImmutableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "catalog_product_tags")
@Immutable
public class CatalogProductTag extends ImmutableEntity {

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "catalog_product_id", nullable = false)
  private CatalogProduct catalogProduct;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tag_id", nullable = false)
  private Tag tag;

  private CatalogProductTag(CatalogProduct catalogProduct, Tag tag) {
    this.catalogProduct = catalogProduct;
    this.tag = tag;
  }

  public static CatalogProductTag of(CatalogProduct catalogProduct, Tag tag) {
    return new CatalogProductTag(catalogProduct, tag);
  }

}
