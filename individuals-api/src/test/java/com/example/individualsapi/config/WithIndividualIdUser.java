package com.example.individualsapi.config;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@WithSecurityContext(factory = IndividualIdSecurityContextFactory.class)
public @interface WithIndividualIdUser {

    /**
     * Идентификатор индивидуального клиента
     * @return
     */
    String value();

}

