package io.github.metdaisy.amaazon.catalog.domain.entity;

import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.common.jpa.MutableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "categories")
public class Category extends MutableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private Category parent;

  @Size(max = 255)
  @NotNull
  @Column(name = "name", nullable = false)
  private String name;

  @NotNull
  @Column(name = "depth", nullable = false)
  private Integer depth;

  @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY,
      cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Category> children = new ArrayList<>();

  private Category(String name, Category parent, Integer depth, List<Category> children) {
    this.name = name;
    this.parent = parent;
    this.depth = depth;
    this.children = children;
  }

  public static Category of(String name, Category parent) {
    Category category = new Category(name, parent, parent == null ? 1 : parent.getDepth() + 1,
        new ArrayList<>());
    if (parent != null) {
      parent.addChild(category);
    }
    return category;
  }

  public void rename(String name) {
    this.name = name;
  }

  public void moveTo(Category parent) {
    validateNoCycle(parent);
    if (this.parent != parent) {
      if (this.parent != null) {
        this.parent.removeChild(this);
      }
      if (parent != null) {
        parent.addChild(this);
      }
    }
    this.parent = parent;
    this.depth = parent == null ? 1 : parent.getDepth() + 1;
  }

  private void validateNoCycle(Category parent) {
    Category current = parent;
    while (current != null) {
      if (current == this || (getId() != null && getId().equals(current.getId()))) {
        throw new CatalogProductException(CatalogProductErrorCode.CATEGORY_CYCLE_DETECTED,
            Map.of("categoryId", getId()));
      }
      current = current.parent;
    }
  }

  public void addChild(Category child) {
    if (!children.contains(child)) {
      children.add(child);
    }
  }

  public void removeChild(Category child) {
    children.remove(child);
  }

  public void updateDepth(int depth) {
    this.depth = depth;
  }

}
