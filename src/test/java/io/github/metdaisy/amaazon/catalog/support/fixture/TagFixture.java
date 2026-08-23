package io.github.metdaisy.amaazon.catalog.support.fixture;

import io.github.metdaisy.amaazon.catalog.domain.entity.Tag;
import java.util.List;

public final class TagFixture {

  private TagFixture() {
  }

  public static Tag tag() {
    return tag("office");
  }

  public static Tag tag(String name) {
    return new Tag(name);
  }

  public static List<Tag> tags() {
    return List.of(tag("office"), tag("sale"), tag("laptop"));
  }
}
