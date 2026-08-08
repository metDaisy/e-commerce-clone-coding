package io.github.metdaisy.amaazon.product.infra.adapter;

import io.github.metdaisy.amaazon.product.application.port.out.ProductUserPort;
import io.github.metdaisy.amaazon.user.application.port.in.UserQueryApi;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductUserAdapter implements ProductUserPort {
  private final UserQueryApi api;

  @Override
  public boolean existsUser(UUID id) {
    return api.existsByUserId(id);
  }
}
