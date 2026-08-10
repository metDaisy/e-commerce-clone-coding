package io.github.metdaisy.amaazon;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.Architectures;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

  private static final String BASE_PACKAGE = "io.github.metdaisy.amaazon";
  private static final List<String> DOMAINS = List.of("auth", "user", "product");

  @Test
  void verifyModularity() {
    // 도메인 간 의존성은 각 도메인의 최상위 package-info.java의
    // @ApplicationModule(allowedDependencies = ...)를 기준으로 검증한다.
    String previousProfile = System.getProperty("SPRING_PROFILES_ACTIVE");
    System.setProperty("SPRING_PROFILES_ACTIVE", "test");
    try {
      ApplicationModules.of(Amaazon.class).verify();
    } finally {
      if (previousProfile == null) {
        System.clearProperty("SPRING_PROFILES_ACTIVE");
      } else {
        System.setProperty("SPRING_PROFILES_ACTIVE", previousProfile);
      }
    }
  }

  @Test
  void verifyLayerArchitecture() {
    JavaClasses importedClasses = importProductionClasses();

    for (String domain : DOMAINS) {
      String domainPackage = BASE_PACKAGE + "." + domain;

      Architectures.layeredArchitecture()
          .consideringOnlyDependenciesInLayers()
          .layer("Presentation").definedBy(domainPackage + ".presentation..")
          .layer("Application").definedBy(domainPackage + ".application..")
          .layer("Domain").definedBy(domainPackage + ".domain..")
          .layer("Infrastructure").definedBy(domainPackage + ".infra..")

          // 도메인 내부 계층 방향: Presentation -> Application -> Domain <- Infrastructure
          .whereLayer("Presentation").mayOnlyBeAccessedByLayers("Presentation")
          // Infrastructure는 DIP에 따라 Application의 outbound port를 구현할 수 있다.
          .whereLayer("Application")
          .mayOnlyBeAccessedByLayers("Presentation", "Application", "Infrastructure")
          .whereLayer("Domain")
          .mayOnlyBeAccessedByLayers("Application", "Domain", "Infrastructure")
          .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Infrastructure")
          .check(importedClasses);
    }
  }

  private JavaClasses importProductionClasses() {
    try {
      Path mainClasses = Paths.get(
          Amaazon.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      return new ClassFileImporter().importPath(mainClasses);
    } catch (URISyntaxException exception) {
      throw new IllegalStateException("프로덕션 클래스 경로를 확인할 수 없습니다.", exception);
    }
  }
}
