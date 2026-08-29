package io.github.metdaisy.amaazon.common.dto;

import java.util.Collections;
import java.util.List;

public record CursorResult<T>(List<T> content, boolean hasNext) {

  public CursorResult {
    content = content == null ? Collections.emptyList() : content;
  }
}
