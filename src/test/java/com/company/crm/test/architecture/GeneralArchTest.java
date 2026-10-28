package com.company.crm.test.architecture;

import com.company.crm.CRMApplication;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.GeneralCodingRules.ASSERTIONS_SHOULD_HAVE_DETAIL_MESSAGE;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

/**
 * Reusable coding rules from ArchUnit's standard library.
 * Import production classes only: tests may deliberately use otherwise forbidden APIs.
 */
@AnalyzeClasses(packagesOf = CRMApplication.class, importOptions = DoNotIncludeTests.class)
@SuppressWarnings("unused") // ArchUnit discovers @ArchTest fields through reflection.
class GeneralArchTest {

    @ArchTest
    static final ArchRule USE_LOGGER_INSTEAD_OF_STANDARD_STREAMS = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
            .because("application diagnostics should go through the configured logging framework");

    @ArchTest
    static final ArchRule USE_SLF4J_INSTEAD_OF_JAVA_UTIL_LOGGING = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING
            .because("application code should use the same logging API as Jmix and Spring Boot");

    @ArchTest
    static final ArchRule ASSERTIONS_HAVE_DETAIL_MESSAGES = ASSERTIONS_SHOULD_HAVE_DETAIL_MESSAGE
            .because("a failed Java assertion should explain which invariant was violated");
}
