package com.company.crm.ai.config;

import com.company.crm.app.config.HttpClientProxyConfiguration;
import com.company.crm.app.util.proxy.ProxyUtils;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiCommonProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({OpenAiCommonProperties.class, OpenAiChatProperties.class})
public class CrmAiConfig {

    private final OpenAiChatProperties chatProperties;
    private final OpenAiCommonProperties commonProperties;

    public CrmAiConfig(OpenAiCommonProperties commonProperties,
                       OpenAiChatProperties chatProperties) {
        this.chatProperties = chatProperties;
        this.commonProperties = commonProperties;
    }

    public boolean isAiIntegrationEnabled() {
        String apiKey = resolveProperty(chatProperties.getApiKey(), commonProperties.getApiKey());
        return StringUtils.hasText(apiKey) && !apiKey.contains("YOUR_API_KEY");
    }

    @Bean
    static BeanPostProcessor openAiProxyConfigurer(ObjectProvider<HttpClientProxyConfiguration> proxyConfigurationProvider) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof OpenAiCommonProperties properties) {
                    HttpClientProxyConfiguration proxyConfiguration = proxyConfigurationProvider.getObject();
                    if (proxyConfiguration.isEnabled()) {
                        properties.setProxy(ProxyUtils.buildProxy(proxyConfiguration));
                    }
                }
                return bean;
            }
        };
    }

    private static String resolveProperty(String modelProperty, String commonProperty) {
        return StringUtils.hasText(modelProperty) ? modelProperty : commonProperty;
    }
}