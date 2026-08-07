package io.github.metdaisy.amaazon.product.domain.entity;

import io.github.metdaisy.amaazon.common.jpa.ImmutableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "product_tags")
@Immutable
public class ProductTag extends ImmutableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tag_id", nullable = false)
  private Tag tag;

  private ProductTag(Product product, Tag tag) {
    this.product = product;
    this.tag = tag;
  }

  public static ProductTag of(Product product, Tag tag) {
    return new ProductTag(product, tag);
  }
}
