package io.github.metdaisy.amaazon;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

  @Test
  void verifyModularity() {
    ApplicationModules.of(Amaazon.class).verify();
  }

  @Test
  void verifyLayerArchitecture() {
    // 우리 프로젝트의 모든 클래스를 불러옵니다.
    JavaClasses importedClasses = new ClassFileImporter()
            .importPackages("io.github.metdaisy.amaazon");
    Architectures.layeredArchitecture()
            .consideringOnlyDependenciesInLayers() // 자바 기본 클래스 등은 무시하고 우리 레이어 간의 의존성만 검사
            // [레이어 정의] 패키지명 기준으로 레이어를 나눕니다.
            .layer("Presentation").definedBy("..presentation..")
            .layer("Application").definedBy("..application..")
            .layer("Domain").definedBy("..domain..")
            .layer("Infrastructure").definedBy("..infra..")

            // [규칙 정의]
            // Presentation은 가장 바깥이므로 아무에게도 참조당하지 않음
            .whereLayer("Presentation").mayNotBeAccessedByAnyLayer()

            // Application은 Presentation에게만 참조될 수 있음 (Presentation -> Application)
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Presentation")

            // Domain은 Presentation, Application, Infra 모두에게 참조될 수 있음
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Presentation", "Application", "Infrastructure")

            // Infra 역시 아무에게도 참조당하지 않음 (의존성 역전에 의해 Application이 Infra를 참조하면 안 됨!)
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()

            // 검증 실행!
            .check(importedClasses);
  }
}
