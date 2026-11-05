# Dynamic Model translator stand

This branch starts from CRM `main` and uses Jmix `3.2.999-SNAPSHOT` from Maven Local.
Publish the Jmix Gradle plugin and required framework/add-on modules to Maven Local
before importing the root `settings.gradle` as a standalone Gradle project. Build
the Gradle plugin with JDK 21 and select JDK 21 as the project/Gradle JVM in the IDE.

1. Run `./gradlew bootRun` and open `http://localhost:8080/b2b-crm`. Log in as `admin` / `admin`.
2. Open **Administration → Dynamic model settings**, select **Code**, and replace the entire editor content
   with `e2e/exported-model.yaml`.
3. Click **Apply**, review and confirm the migration, then reload the page.
   The E2E menu exposes contracts and notes;
   a contract's detail view includes its lines. Use these views to create sample data.
4. In the settings view, **Copy to clipboard** exports the complete YAML. Save it to a project file
   (or use the committed fixture), then stop the application before generating static code.
5. In Jmix Studio, use **Convert Dynamic Model to Code...** on the YAML: Java,
   `com.company.crm.model.e2e`, **Generate views** enabled. All three entities and the enumeration
   are converted; inspect the report and generated files.

Before restarting with generated code, handle schema/data migration and switching the dynamic
definition yourself. Automatic runtime handover is outside the converter.

Smoke tests: `./gradlew test --tests '*DynamicModelSettingsViewTest' --tests '*LoginViewTest'`.

Local readiness check (2026-11-05): the fixture is applied in `.jmix/hsqldb/b2b-crm`.
The sample contract `E2E-SMOKE-20261105` has one line; it was saved and reopened in the browser.
Both note views and the settings Code/Copy actions were also opened. These local data are not in Git.
