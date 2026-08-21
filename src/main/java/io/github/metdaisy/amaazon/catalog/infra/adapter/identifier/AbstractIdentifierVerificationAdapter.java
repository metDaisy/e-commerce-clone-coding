package io.github.metdaisy.amaazon.catalog.infra.adapter.identifier;


import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogProductIdentifierType;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductErrorCode;
import io.github.metdaisy.amaazon.catalog.domain.exception.CatalogProductException;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import io.github.metdaisy.amaazon.catalog.domain.verifier.CatalogProductIdentifierVerifier;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractIdentifierVerificationAdapter implements
    CatalogProductIdentifierVerifier {

  private final CatalogProductRepository repository;
  private final CatalogProductIdentifierType type;

  @Override
  public boolean support(CatalogProductIdentifierType type) {
    return this.type.equals(type);
  }

  @Override
  public void verify(UUID id, String identifierValue) {
    if (repository.existsIdentifier(id, type, identifierValue)) {
      throw new CatalogProductException(CatalogProductErrorCode.PRODUCT_CODE_ERROR,
          AmaazonExceptionContext.logDetails(Map.of(type.name(), identifierValue)));
    }
  }
}
