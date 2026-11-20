package dev.studyforge.insights;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Same hexagonal rules as learning-service. */
@AnalyzeClasses(
    packages = "dev.studyforge.insights",
    importOptions = ImportOption.DoNotIncludeTests.class)
class LayeringTest {

  @ArchTest
  static final ArchRule domainIsPureJava =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework..",
              "tools.jackson..",
              "java.sql..",
              "jakarta..",
              "org.apache.kafka..",
              "dev.studyforge.contracts..",
              "..application..",
              "..adapter..");

  @ArchTest
  static final ArchRule applicationDoesNotKnowAdaptersOrInfrastructure =
      noClasses()
          .that()
          .resideInAPackage("..application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..adapter..",
              "org.springframework.jdbc..",
              "org.springframework.web..",
              "org.springframework.kafka..",
              "org.apache.kafka..");

  @ArchTest
  static final ArchRule webDoesNotTouchMessagingOrPersistence =
      noClasses()
          .that()
          .resideInAPackage("..adapter.web..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..adapter.persistence..", "..adapter.messaging..");
}
