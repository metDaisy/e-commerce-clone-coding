package io.github.metdaisy.amaazon.catalog.infra.adapter;

import io.github.metdaisy.amaazon.catalog.application.port.out.CatalogSellerPort;
import io.github.metdaisy.amaazon.seller.application.port.in.SellerQueryApi;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CatalogSellerAdapter implements CatalogSellerPort {

  private final SellerQueryApi api;

  @Override
  public boolean existsSeller(UUID id) {
    return api.isActiveSeller(id);
  }
}
