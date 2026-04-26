package com.example.transactionservice.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@ConditionalOnClass(DataSource.class)
public class DataSourceDependencyBeanPostProcessor implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        if (registry.containsBeanDefinition("flywayExecutor")) {
            BeanDefinition dataSource = registry.getBeanDefinition("dataSource");
            dataSource.setDependsOn("flywayExecutor");
        }
    }
}
