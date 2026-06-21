package com.example.currencyrateservice.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class LockProviderConfig {

    @Bean
    LockProvider lockProvider(JdbcTemplate jdbcTemplate){
        return new JdbcTemplateLockProvider(jdbcTemplate);
    }
}
