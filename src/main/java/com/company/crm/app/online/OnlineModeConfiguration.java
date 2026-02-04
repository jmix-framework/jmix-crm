package com.company.crm.app.online;

import com.company.crm.app.annotation.OnlineProfile;
import io.jmix.autoconfigure.data.JmixLiquibaseCreator;
import liquibase.integration.spring.SpringLiquibase;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

/**
 * Configuration of beans in online demo mode.
 */
@Configuration
@EnableScheduling
@OnlineProfile
public class OnlineModeConfiguration {

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "routing.datasource")
    public DataSource dataSource() {
        return new RoutingDataSource();
    }

    @Bean("crm_SessionDataSource")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    @ConfigurationProperties(prefix = "session.datasource")
    public DataSource sessionDataSource() {
        return new BasicDataSource();
    }

    @Bean(name = "jmix_Liquibase")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public SpringLiquibase liquibase(DataSource dataSource,
                                     @Qualifier("jmix_LiquibaseProperties") LiquibaseProperties properties) {
        return JmixLiquibaseCreator.create(dataSource, properties);
    }
}