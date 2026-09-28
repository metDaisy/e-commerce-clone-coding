package io.github.metdaisy.amaazon.cart.application.dto;

import java.util.List;
import java.util.UUID;

public record CartMergeResult(Status status, UUID cartId, List<UUID> mergedCartItemIds,
                              List<UUID> conflictOfferIds) {

  public enum Status {
    MERGED, CONFLICT
  }

  public CartMergeResult {
    mergedCartItemIds = List.copyOf(mergedCartItemIds);
    conflictOfferIds = List.copyOf(conflictOfferIds);
  }

  public boolean merged() {
    return status == Status.MERGED;
  }

  public String exceptionCode() {
    return merged() ? null : "CART-007";
  }
}
