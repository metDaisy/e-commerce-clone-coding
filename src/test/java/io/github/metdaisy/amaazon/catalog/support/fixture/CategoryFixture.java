package io.github.metdaisy.amaazon.catalog.support.fixture;

import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import java.util.List;

public final class CategoryFixture {

  private CategoryFixture() {
  }

  public static Category category() {
    return category("Computers");
  }

  public static Category category(String name) {
    return Category.of(name, null);
  }

  public static List<Category> categories() {
    return List.of(category("Computers"), category("Books"), category("Home"));
  }
}
