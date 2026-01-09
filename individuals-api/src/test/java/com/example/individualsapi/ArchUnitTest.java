package com.example.individualsapi;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

public class ArchUnitTest {

    JavaClasses importedClasses = new ClassFileImporter().importPackages("com.example.individualsapi");

    @Test
    void services_should_reside_in_service_package() {
        classes().that().areAnnotatedWith(Service.class)
                .or().haveNameMatching(".*ServiceImpl")
                .should().resideInAPackage("..service..")
                .check(importedClasses);
    }

    @Test
    void controllers_should_reside_in_controller_package() {
        classes().that().areAnnotatedWith(RestController.class)
                .should().resideInAPackage("..controller..")
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
    void classes_should_not_throw_generic_exception() {
        NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS.check(importedClasses);
    }

    @Test
    void classes_should_not_use_sout() {
        NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(importedClasses);
    }
}
