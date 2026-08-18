package io.github.metdaisy.amaazon.common.dto;

import java.util.List;

public record PageResponse<T>(List<T> data, int page, int size, int totalElements, int totalPages) {

}
