package com.example.personservice;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
import static com.tngtech.archunit.library.ProxyRules.no_classes_should_directly_call_other_methods_declared_in_the_same_class_that_are_annotated_with;

public class ArchUnitTest {

    JavaClasses importedClasses = new ClassFileImporter().importPackages("com.example.personservice");

    @Test
    void repositories_should_reside_in_repository_package() {
        classes().that().areAnnotatedWith(Repository.class)
                .or().haveNameMatching(".*Repository")
                .should()
                .resideInAPackage("..repository..")
                .check(importedClasses);
    }

    @Test
    void services_should_reside_in_service_package() {
        classes().that().areAnnotatedWith(Service.class)
                .or().haveNameMatching(".*ServiceImpl")
                .should().resideInAPackage("..service..")
                .check(importedClasses);
    }

    @Test
    void entities_should_reside_in_entity_package() {
        classes().that().areAnnotatedWith(Entity.class)
                .should().resideInAPackage("..entity..")
                .check(importedClasses);
    }

    @Test
    void controllers_should_reside_in_controller_package() {
        classes().that().areAnnotatedWith(RestController.class)
                .should().resideInAPackage("..controller..")
                .check(importedClasses);
    }

    @Test
    void controllers_should_not_directly_use_repository() {
        noClasses().that().areAnnotatedWith(RestController.class)
                .should().dependOnClassesThat().areAnnotatedWith(Repository.class)
                .check(importedClasses);
    }

    @Test
    void controllers_should_not_represent_entity() {
        methods().that().areDeclaredInClassesThat().resideInAPackage("..controller..")
                .should().notHaveRawReturnType(resideInAPackage("..entity.."))
                .check(importedClasses);
    }

    @Test
    void interfaces_should_not_have_names_ending_with_word_interface() {
        noClasses().that().areInterfaces()
                .should().haveNameMatching(".*Interface")
                .check(importedClasses);
    }

    @Test
    void interfaces_should_not_have_simple_class_name_containing_the_word_interface() {
        noClasses().that().areInterfaces()
                .should().haveSimpleNameContaining("Interface")
                .check(importedClasses);
    }

    @Test
    void interfaces_must_not_be_placed_in_implementation_package() {
        noClasses().that().resideInAPackage("..impl..")
                .should().beInterfaces()
                .check(importedClasses);
    }

    @Test
    void no_bypass_of_proxy_logic() {
        no_classes_should_directly_call_other_methods_declared_in_the_same_class_that_are_annotated_with(Transactional.class);
    }

    @Test
    void classes_should_not_throw_generic_exception() {
        NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS.check(importedClasses);
    }

    @Test
    void classes_should_not_use_sout() {
        NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(importedClasses);
    }
}
