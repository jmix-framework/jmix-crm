package com.company.crm.test.architecture;

import com.company.crm.CRMApplication;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.annotation.JmixProperty;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;

import java.beans.Introspector;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Select entities by annotations so the rules also cover DTOs and entities outside a single model package.
 */
@AnalyzeClasses(packagesOf = CRMApplication.class, importOptions = DoNotIncludeTests.class)
@SuppressWarnings("unused") // ArchUnit discovers @ArchTest fields through reflection.
class EntityArchTest {

    @ArchTest
    static final ArchRule JPA_MODEL_IS_REGISTERED_IN_JMIX = classes()
            .that().areAnnotatedWith(Entity.class)
            .or().areAnnotatedWith(Embeddable.class)
            .or().areAnnotatedWith(MappedSuperclass.class)
            .should().beAnnotatedWith(JmixEntity.class)
            .because("Jmix needs metadata for persistent entities, embedded values and their base classes");

    @ArchTest
    static final ArchRule ENTITIES_DO_NOT_DEPEND_ON_UI = noClasses()
            .that().areAnnotatedWith(JmixEntity.class)
            .should().dependOnClassesThat().resideInAnyPackage("com.vaadin..", "io.jmix.flowui..")
            .because("the data model should be usable from services, REST endpoints and background jobs without UI types");

    @ArchTest
    static final ArchRule COMPUTED_GETTERS_WITH_DEPENDENCIES_HAVE_METADATA = methods()
            .that().areDeclaredInClassesThat().areAnnotatedWith(JmixEntity.class)
            .and().haveNameStartingWith("get")
            .and().areAnnotatedWith(DependsOnProperties.class)
            .and(describe("have no backing field", method -> method.getOwner().getAllFields().stream()
                    .noneMatch(field -> field.getName().equals(Introspector.decapitalize(method.getName().substring(3))))))
            .should().beAnnotatedWith(JmixProperty.class)
            .orShould().beAnnotatedWith(InstanceName.class)
            .because("DataContext needs a metadata property when it propagates changes to a computed getter");
}
