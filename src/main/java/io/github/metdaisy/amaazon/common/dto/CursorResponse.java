package io.github.metdaisy.amaazon.common.dto;

import java.util.List;

public record CursorResponse<T>(List<T> data, boolean hasNext, String nextCursor) {

}
