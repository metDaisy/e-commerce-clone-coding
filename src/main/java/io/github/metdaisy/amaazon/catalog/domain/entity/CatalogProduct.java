package io.github.metdaisy.amaazon.catalog.domain.entity;

import io.github.metdaisy.amaazon.catalog.domain.entity.constant.ProductPublicationStatus;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
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
@Table(name = "catalog_products")
@SQLDelete(sql = "update catalog_products "
    + "set publication_status = 'ARCHIVED', archived_at = now() "
    + "where id = ?")
public class CatalogProduct extends MutableEntity {

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private Category category;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "tag", cascade = {CascadeType.PERSIST,
      CascadeType.REMOVE}, orphanRemoval = true)
  private List<CatalogProductTag> tags = new ArrayList<>();

  @NotNull
  @Column(name = "manager_id", nullable = false)
  private UUID managerId;

  @Size(max = 255)
  @NotNull
  @Column(name = "name", nullable = false)
  private String name;

  @NotNull
  @Column(name = "description", nullable = false, length = Integer.MAX_VALUE)
  private String description;

  @Size(max = 255)
  @Column(name = "brand")
  private String brand;

  @Size(max = 50)
  @Column(name = "asin", length = 50)
  private String asin;

  @Size(max = 50)
  @Column(name = "gtin", length = 50)
  private String gtin;

  @Size(max = 50)
  @Column(name = "upc", length = 50)
  private String upc;

  @Size(max = 50)
  @Column(name = "ean", length = 50)
  private String ean;

  @Size(max = 50)
  @Column(name = "isbn", length = 50)
  private String isbn;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "attributes")
  private Map<String, Object> attributes = new HashMap<>();

  @Size(max = 20)
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "publication_status", nullable = false, length = 20)
  private ProductPublicationStatus publicationStatus;

  @Column(name = "archived_at")
  private Instant archivedAt;

  @Builder
  private CatalogProduct(Category category, UUID managerId, String name, String description,
      String brand, String asin, String gtin, String upc, String ean, String isbn,
      Map<String, Object> attributes, List<CatalogProductTag> tags) {
    this.category = category;
    this.managerId = managerId;
    this.name = name;
    this.description = description;
    this.brand = brand;
    this.asin = asin;
    this.gtin = gtin;
    this.upc = upc;
    this.ean = ean;
    this.isbn = isbn;
    this.attributes = attributes;
    this.publicationStatus = ProductPublicationStatus.ACTIVE;
    this.archivedAt = null;
    this.tags = tags;
  }
}
