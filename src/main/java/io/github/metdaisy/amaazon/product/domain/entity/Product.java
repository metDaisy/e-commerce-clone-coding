package io.github.metdaisy.amaazon.product.domain.entity;

import io.github.metdaisy.amaazon.common.jpa.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ColumnDefault;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "products")
public class Product extends MutableEntity {

  @NotNull
  @ManyToOne
  @JoinColumn(name = "category_id")
  private Category category;

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ProductTag> productTags = new HashSet<>();

  @NotNull
  @Column(name = "manager_id", nullable = false)
  private UUID managerId;

  @Size(max = 255)
  @NotNull
  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description", length = Integer.MAX_VALUE)
  private String description;

  @NotNull
  @Column(name = "price", nullable = false)
  private Integer price;

  @Column(name = "sale_price")
  private Integer salePrice;

  @Column(name = "sale_start_at")
  private Instant saleStartAt;

  @Column(name = "sale_end_at")
  private Instant saleEndAt;

  @Size(max = 50)
  @NotNull
  @Column(name = "status", nullable = false, length = 50)
  @Enumerated(value = EnumType.STRING)
  private ProductStatus status;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "stock_quantity", nullable = false)
  private Integer stockQuantity;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "view_count", nullable = false)
  private Integer viewCount;

  @NotNull
  @ColumnDefault("false")
  @Column(name = "is_time_sale", nullable = false)
  private Boolean isTimeSale;

  @Builder
  private Product(Category category,
      Set<ProductTag> productTags,
      UUID managerId,
      String name,
      String description,
      Integer price,
      Integer salePrice,
      Instant saleStartAt,
      Instant saleEndAt,
      ProductStatus status,
      Integer stockQuantity) {
    this.category = category;
    this.productTags = productTags;
    this.managerId = managerId;
    this.name = name;
    this.description = description;
    this.price = price;
    this.salePrice = salePrice;
    this.saleStartAt = saleStartAt;
    this.saleEndAt = saleEndAt;
    this.status = status;
    this.stockQuantity = stockQuantity;
    this.viewCount = 0;
    this.isTimeSale = false;
  }

}
