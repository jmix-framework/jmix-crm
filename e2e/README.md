# Dynamic Model translator stand

This branch starts from CRM `main` and uses Jmix `3.2.999-SNAPSHOT` from Maven Local.
Publish the Jmix Gradle plugin and required framework/add-on modules to Maven Local
before importing the root `settings.gradle` as a standalone Gradle project. Build
the Gradle plugin with JDK 21 and select JDK 21 as the project/Gradle JVM in the IDE.

The input is `exported-model.yaml`. In Jmix Studio, use **Convert Dynamic Model to Code**
with Java and Generate views enabled. The converter processes all three entities and
the enumeration. Generate into `com.company.crm.model.e2e`, then inspect the report
and generated code before creating the database changelog.

Verification: `./gradlew classes test --tests '*LoginViewTest'`.
