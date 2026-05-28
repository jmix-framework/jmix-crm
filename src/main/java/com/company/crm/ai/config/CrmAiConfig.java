package com.company.crm.ai.config;

import com.company.crm.app.config.HttpClientProxyConfiguration;
import com.company.crm.app.config.RestConfig;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiConnectionProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingProperties;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({OpenAiConnectionProperties.class, OpenAiChatProperties.class})
public class CrmAiConfig {

    private final ApplicationContext applicationContext;
    private final HttpClientProxyConfiguration httpClientProxyConfiguration;

    private final OpenAiChatProperties chatProperties;
    private final OpenAiConnectionProperties connectionProperties;

    public CrmAiConfig(ApplicationContext applicationContext,
                       HttpClientProxyConfiguration httpClientProxyConfiguration,
                       OpenAiConnectionProperties connectionProperties,
                       OpenAiChatProperties chatProperties) {
        this.applicationContext = applicationContext;
        this.httpClientProxyConfiguration = httpClientProxyConfiguration;
        this.chatProperties = chatProperties;
        this.connectionProperties = connectionProperties;
    }

    public boolean isAiIntegrationEnabled() {
        String apiKey = resolveProperty(chatProperties.getApiKey(), connectionProperties.getApiKey());
        return StringUtils.hasText(apiKey) && !apiKey.contains("YOUR_API_KEY");
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "openai", matchIfMissing = true)
    public OpenAiApi openAiApi(ObjectProvider<RestClient.Builder> restClientBuilderProvider,
                               ObjectProvider<WebClient.Builder> webClientBuilderProvider,
                               ResponseErrorHandler responseErrorHandler) {
        String baseUrl = resolveProperty(chatProperties.getBaseUrl(), connectionProperties.getBaseUrl());
        String apiKey = resolveProperty(chatProperties.getApiKey(), connectionProperties.getApiKey());

        Assert.hasText(baseUrl, "OpenAI base URL must be set.");
        Assert.hasText(apiKey, "OpenAI API key must be set.");

        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .headers(resolveHeaders())
                .completionsPath(chatProperties.getCompletionsPath())
                .embeddingsPath(OpenAiEmbeddingProperties.DEFAULT_EMBEDDINGS_PATH)
                .restClientBuilder(RestConfig.makeRestClientBuilder(
                        restClientBuilderProvider.getIfAvailable(RestClient::builder),
                        applicationContext,
                        httpClientProxyConfiguration.isEnabled()))
                .webClientBuilder(webClientBuilderProvider.getIfAvailable(WebClient::builder))
                .responseErrorHandler(responseErrorHandler)
                .build();
    }

    private MultiValueMap<String, String> resolveHeaders() {
        LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        addHeader(headers, "OpenAI-Project",
                resolveProperty(chatProperties.getProjectId(), connectionProperties.getProjectId()));
        addHeader(headers, "OpenAI-Organization",
                resolveProperty(chatProperties.getOrganizationId(), connectionProperties.getOrganizationId()));
        return headers;
    }

    private static void addHeader(LinkedMultiValueMap<String, String> headers, String name, String value) {
        if (StringUtils.hasText(value)) {
            headers.put(name, List.of(value));
        }
    }

    private static String resolveProperty(String modelProperty, String commonProperty) {
        return StringUtils.hasText(modelProperty) ? modelProperty : commonProperty;
    }
}