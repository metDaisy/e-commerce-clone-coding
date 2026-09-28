package io.github.metdaisy.amaazon.cart.infra.adapter;

import io.github.metdaisy.amaazon.cart.application.port.out.OfferQueryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * P9 공개 adapter가 연결되기 전에도 P3가 내부 persistence에 의존하지 않도록 하는 기본 adapter다.
 */
@Component
public class UnavailableOfferQueryAdapter implements OfferQueryPort {

  @Override
  public Optional<OfferSnapshot> findById(UUID offerId) {
    return Optional.empty();
  }
}
