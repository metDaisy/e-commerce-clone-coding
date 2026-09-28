package io.github.metdaisy.amaazon.cart.application.dto;

import java.util.UUID;

public record AddCartItemRequest(UUID offerId, Integer quantity) {
}
