package io.github.metdaisy.amaazon.product.application.dto;

import java.util.List;

public record ProductCreateRequest(String category,
                                   List<String> tags,
                                   String name,
                                   String description,
                                   Integer price) {

}
