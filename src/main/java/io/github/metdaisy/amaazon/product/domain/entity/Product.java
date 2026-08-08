package io.github.metdaisy.amaazon.product.domain.entity;

import io.github.metdaisy.amaazon.common.jpa.MutableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLDelete;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "products")
@SQLDelete(sql = "UPDATE products SET is_deleted = true, updated_at = now() WHERE id = ?")
public class Product extends MutableEntity {

  @NotNull
  @ManyToOne
  @JoinColumn(name = "category_id")
  private Category category;

  @Setter
  @OneToMany(mappedBy = "product", fetch = FetchType.LAZY,
      cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ProductTag> productTags = new HashSet<>();

  @NotNull
  @Column(name = "manager_id", nullable = false)
  private UUID managerId;

  @Size(max = 255)
  @NotNull
  @Column(name = "name", nullable = false)
  private String name;

  @NotNull
  @Column(name = "description", length = Integer.MAX_VALUE, nullable = false)
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

  @Builder(access = AccessLevel.PRIVATE)
  private Product(Category category,
      Collection<ProductTag> productTags,
      UUID managerId,
      String name,
      String description,
      Integer price,
      Integer salePrice,
      Instant saleStartAt,
      Instant saleEndAt,
      ProductStatus status,
      Integer stockQuantity,
      boolean isTimeSale) {
    this.category = category;
    this.productTags = new HashSet<>(productTags);
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
    this.isTimeSale = isTimeSale;
  }

  public static Product create(Category category,
      UUID managerId,
      String name,
      String description,
      Integer price) {
    return Product.builder()
        .category(category)
        .managerId(managerId)
        .name(name)
        .description(description)
        .price(price)
        .status(ProductStatus.ON_SALE)
        .stockQuantity(0)
        .build();
  }
}
