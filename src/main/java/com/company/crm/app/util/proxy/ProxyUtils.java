package com.company.crm.app.util.proxy;

import com.company.crm.app.config.HttpClientProxyConfiguration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.Locale;
import java.util.Objects;

import static com.company.crm.app.config.HttpClientProxyConfiguration.PROPERTIES_PREFIX;

public final class ProxyUtils {
    private ProxyUtils() {
    }

    public static void configureProxy(HttpClientProxyConfiguration proxyConfiguration,
                                      SimpleClientHttpRequestFactory requestFactory) {
        requestFactory.setProxy(buildProxy(proxyConfiguration));
    }

    /**
     * Validates the proxy configuration, registers the JVM-wide proxy {@link Authenticator}
     * when credentials are set, and returns the configured {@link Proxy}.
     */
    public static Proxy buildProxy(HttpClientProxyConfiguration proxyConfiguration) {
        if (!proxyConfiguration.isEnabled()) {
            throw new IllegalStateException(
                    String.format("Property '%s' must be set to true to enable proxy",
                            buildHttpClientProxyPropertyKey("enabled"))
            );
        }

        String proxyHost = proxyConfiguration.getHost();
        if (proxyHost == null || proxyHost.isBlank()) {
            throw new IllegalStateException(
                    String.format("Property '%s' must be set when proxy is enabled",
                            buildHttpClientProxyPropertyKey("host"))
            );
        }

        Integer proxyPort = proxyConfiguration.getPort();
        if (proxyPort == null || proxyPort < 1 || proxyPort > 65535) {
            throw new IllegalStateException(
                    String.format("Property '%s' must be in range [1, 65535] when proxy is enabled",
                            buildHttpClientProxyPropertyKey("port"))
            );
        }

        Proxy.Type proxyType = proxyConfiguration.getType() == HttpClientProxyConfiguration.ProxyType.SOCKS5
                ? Proxy.Type.SOCKS
                : Proxy.Type.HTTP;

        configureProxyAuthenticator(proxyConfiguration, proxyHost, proxyPort);
        return new Proxy(proxyType, new InetSocketAddress(proxyHost, proxyPort));
    }

    private static void configureProxyAuthenticator(HttpClientProxyConfiguration proxyConfiguration,
                                                    String proxyHost,
                                                    int proxyPort) {
        String username = proxyConfiguration.getUsername();
        String password = proxyConfiguration.getPassword();
        boolean usernamePresent = username != null && !username.isBlank();
        boolean passwordPresent = password != null && !password.isBlank();

        if (!usernamePresent && !passwordPresent) {
            return;
        }
        if (!usernamePresent || !passwordPresent) {
            throw new IllegalStateException(
                    String.format("Both properties '%s' and '%s' must be set together",
                            PROPERTIES_PREFIX + "." + buildHttpClientProxyPropertyKey("username"),
                            PROPERTIES_PREFIX + "." + buildHttpClientProxyPropertyKey("password"))
            );
        }

        String proxyUsername = Objects.requireNonNull(username).strip();
        char[] proxyPassword = Objects.requireNonNull(password).toCharArray();

        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                if (isMatchingProxyRequest(proxyHost, proxyPort, getRequestingHost(),
                        getRequestingSite(), getRequestingPort(), getRequestingProtocol(), getRequestorType())) {
                    return new PasswordAuthentication(proxyUsername, proxyPassword);
                }
                return super.getPasswordAuthentication();
            }
        });
    }

    static boolean isMatchingProxyRequest(String proxyHost,
                                          int proxyPort,
                                          String requestingHost,
                                          InetAddress requestingSite,
                                          int requestingPort,
                                          String requestingProtocol,
                                          Authenticator.RequestorType requestorType) {
        boolean hostMatches = proxyHost.equalsIgnoreCase(requestingHost)
                || (requestingSite != null
                && (proxyHost.equalsIgnoreCase(requestingSite.getHostName())
                || proxyHost.equalsIgnoreCase(requestingSite.getHostAddress())));

        boolean portMatches = requestingPort == proxyPort || requestingPort == -1;
        boolean protocolIsSocks = requestingProtocol != null && requestingProtocol.toUpperCase(Locale.ROOT).startsWith("SOCKS");
        boolean proxyTypeRequest = requestorType == Authenticator.RequestorType.PROXY;

        return hostMatches && portMatches && (proxyTypeRequest || protocolIsSocks);
    }

    private static String buildHttpClientProxyPropertyKey(String field) {
        return PROPERTIES_PREFIX + "." + field;
    }
}