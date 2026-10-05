package org.example.knockin.global.config;

import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.jpa.show-sql", havingValue = "false")
public class DataSourceProxyConfig {

    private static final String LOGGER_NAME = "org.example.knockin.query";

    @Bean
    static BeanPostProcessor dataSourceProxyBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(@NotNull Object bean, @NotNull String beanName) {
                if (!(bean instanceof DataSource dataSource) || bean instanceof ProxyDataSource) {
                    return bean;
                }

                return ProxyDataSourceBuilder.create(dataSource)
                        .name(beanName)
                        .logQueryBySlf4j(SLF4JLogLevel.INFO, LOGGER_NAME)
                        .build();
            }
        };
    }
}