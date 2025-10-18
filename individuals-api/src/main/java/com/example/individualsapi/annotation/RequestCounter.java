package com.example.individualsapi.annotation;

import java.lang.annotation.*;

/**
 * Аннотация для сборка метрик MetricRegistry о количестве запросов
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestCounter {

    /**
     * Название метода для сбора метрик
     *
     * @return Название метода
     */
    MetricNames metric();


}
