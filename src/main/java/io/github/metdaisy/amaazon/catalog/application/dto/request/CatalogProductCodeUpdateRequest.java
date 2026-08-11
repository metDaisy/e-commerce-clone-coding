package io.github.metdaisy.amaazon.catalog.application.dto.request;

public record CatalogProductCodeUpdateRequest(String asin, String gtin, String upc, String ean,
                                              String isbn, SignedCatalogCode catalogCode) {

  public record SignedCatalogCode(String asin, String gtin, String upc, String ean,
                                  String isbn) {

  }
}
