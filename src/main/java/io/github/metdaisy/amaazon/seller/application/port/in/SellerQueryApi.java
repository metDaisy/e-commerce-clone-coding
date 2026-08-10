package io.github.metdaisy.amaazon.seller.application.port.in;

import io.github.metdaisy.amaazon.seller.domain.entity.constant.SellerStatus;
import io.github.metdaisy.amaazon.seller.domain.repository.SellerRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@NamedInterface("api")
@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SellerQueryApi {

  private final SellerRepository repository;

  public boolean isActiveSeller(UUID id) {
    return repository.existsByIdAndStatus(id, SellerStatus.ACTIVE);
  }
}
