package dev.studyforge.learning;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Hexagonal layering: the domain is plain Java, the application never sees adapters. */
@AnalyzeClasses(
    packages = "dev.studyforge.learning",
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
              "com.fasterxml..",
              "java.sql..",
              "jakarta..",
              "org.apache.kafka..",
              "dev.studyforge.contracts..")
          .because("scheduling and progress rules are plain Java records and interfaces");

  @ArchTest
  static final ArchRule domainDoesNotReachOutwards =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..application..", "..adapter..");

  @ArchTest
  static final ArchRule applicationDoesNotKnowAdapters =
      noClasses()
          .that()
          .resideInAPackage("..application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..adapter..",
              "org.springframework.jdbc..",
              "org.springframework.web..",
              "org.apache.kafka..",
              "org.springframework.kafka..");

  @ArchTest
  static final ArchRule adaptersDoNotTalkToEachOther =
      noClasses()
          .that()
          .resideInAPackage("..adapter.web..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..adapter.persistence..", "..adapter.messaging..");
}
