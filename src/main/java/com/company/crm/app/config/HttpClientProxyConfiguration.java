package com.company.crm.app.config;

import org.jetbrains.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = HttpClientProxyConfiguration.PROPERTIES_PREFIX)
public class HttpClientProxyConfiguration {

    public static final String PROPERTIES_PREFIX = "crm.http.client.proxy";

    private Boolean enabled = false;
    @Nullable
    private String host;
    @Nullable
    private Integer port;
    @Nullable
    private String username;
    @Nullable
    private String password;
    private ProxyType type = ProxyType.SOCKS5;

    public enum ProxyType {
        HTTP, SOCKS5
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ProxyType getType() {
        return type;
    }

    public void setType(ProxyType type) {
        this.type = type;
    }

    @Nullable
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Nullable
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Nullable
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Nullable
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}