package io.github.metdaisy.amaazon.catalog.domain.entity;

import io.github.metdaisy.amaazon.common.jpa.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "categories")
@Immutable
@Cache(region = "Category", usage = CacheConcurrencyStrategy.READ_ONLY)
public class Category extends MutableEntity {

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private Category parent;

  @Size(max = 255)
  @NotNull
  @Column(name = "name", nullable = false)
  private String name;

  @NotNull
  @Column(name = "depth", nullable = false)
  private Integer depth;

  @OneToMany(mappedBy = "categories", fetch = FetchType.LAZY)
  private List<Category> childrenCategory = new ArrayList<>();

}
