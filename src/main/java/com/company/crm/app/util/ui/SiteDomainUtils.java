package com.company.crm.app.util.ui;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Detects the public domain the application is served from and the site language it implies.
 * <p>
 * The language follows the domain, not the user locale: jmix.io is the English site, jmix.ru is the
 * Russian one. Any other host (local runs, previews, forks deployed elsewhere) implies no language.
 */
public final class SiteDomainUtils {

    public static final String DOMAIN_EN = "jmix.io";
    public static final String DOMAIN_RU = "jmix.ru";

    private static final Locale LOCALE_RU = Locale.of("ru");

    private SiteDomainUtils() {
    }

    /**
     * @return the site language of the current Vaadin request, or {@code null} if there is no request
     * or it comes from an unknown domain
     */
    @Nullable
    public static Locale getCurrentRequestLocale() {
        return resolveLocale(getCurrentRequestHost());
    }

    /**
     * @param host lower-case host name without a port, or {@code null} if it could not be determined
     * @return {@link Locale#ENGLISH} for jmix.io, Russian for jmix.ru, {@code null} for any other host
     */
    @Nullable
    public static Locale resolveLocale(@Nullable String host) {
        if (host == null) {
            return null;
        }

        if (isWithinDomain(host, DOMAIN_EN)) {
            return Locale.ENGLISH;
        }
        if (isWithinDomain(host, DOMAIN_RU)) {
            return LOCALE_RU;
        }
        return null;
    }

    public static boolean isWithinDomain(String host, String domain) {
        return host.equals(domain) || host.endsWith("." + domain);
    }

    /**
     * @return the client-facing host of the current Vaadin request, or {@code null} if there is no request
     */
    @Nullable
    public static String getCurrentRequestHost() {
        VaadinRequest request = VaadinService.getCurrentRequest();
        if (request == null) {
            return null;
        }

        // behind a reverse proxy the original host arrives in X-Forwarded-Host
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        return normalizeHost(forwardedHost != null ? forwardedHost : request.getHeader("Host"));
    }

    /**
     * Reduces a {@code Host} / {@code X-Forwarded-Host} header value to a bare lower-case host name:
     * a chain of proxies appends to the header, so only the first entry is the original client-facing
     * host, and it may carry a port.
     */
    @Nullable
    public static String normalizeHost(@Nullable String hostHeader) {
        if (hostHeader == null) {
            return null;
        }

        // limit 2 keeps a leading empty entry, so a header like "," or ":" yields "" instead of throwing
        String host = hostHeader.split(",", 2)[0]
                .trim()
                .split(":", 2)[0]
                .toLowerCase(Locale.ROOT);

        return host.isEmpty() ? null : host;
    }
}
