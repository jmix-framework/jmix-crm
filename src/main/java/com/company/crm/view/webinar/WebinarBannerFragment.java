package com.company.crm.view.webinar;

import com.company.crm.app.util.ui.SiteDomainUtils;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Locale;
import java.util.Set;

/**
 * Webinar banner. It brings its own stylesheet, so embedding it touches no shared theme file.
 * <p>
 * Clicking the banner opens {@link #setUrl(String) url} in a new tab. The banner is shown only where both
 * {@link #setDomain(Domain) domain} and the language filters match. By default, all domains and languages match.
 * {@link #setSupportedLanguages(Set) Supported languages} allow multiple languages; an empty set allows all.
 * {@link #setExcludedLanguages(Set) Excluded languages} take precedence. Values in XML are separated by commas or spaces.
 * The legacy {@link #setLanguage(Language) language} property remains an additional filter, defaulting to {@code ANY}.
 * Set them on the embedding element:
 * <pre>{@code
 * <fragment id="webinarBannerFragment" class="com.company.crm.view.webinar.WebinarBannerFragment">
 *     <properties>
 *         <property name="url" value="https://forms.jmix.io/some-webinar?utm_source=banner&amp;utm_medium=site"/>
 *         <property name="domain" value="JMIX_IO"/>
 *         <property name="supportedLanguages" value="EN, DE"/>
 *         <property name="excludedLanguages" value="RU"/>
 *     </properties>
 * </fragment>
 * }</pre>
 */
@StyleSheet("themes/aura/view/webinar.css")
@FragmentDescriptor("webinar-banner-fragment.xml")
public class WebinarBannerFragment extends Fragment<VerticalLayout> {

    /**
     * Site the application is served from; a subdomain matches too, e.g. demo.jmix.io for {@link #JMIX_IO}.
     */
    public enum Domain {
        ANY(null),
        JMIX_IO(SiteDomainUtils.DOMAIN_EN),
        JMIX_RU(SiteDomainUtils.DOMAIN_RU);

        @Nullable
        private final String name;

        Domain(@Nullable String name) {
            this.name = name;
        }

        boolean matches(@Nullable String host) {
            return name == null || (host != null && SiteDomainUtils.isWithinDomain(host, name));
        }
    }

    /**
     * UI language of the current user, one per {@code jmix.core.available-locales} entry.
     */
    public enum Language {
        ANY(null),
        EN("en"),
        DE("de"),
        RU("ru"),
        IT("it"),
        ES("es"),
        VI("vi"),
        SR("sr");

        @Nullable
        private final String code;

        Language(@Nullable String code) {
            this.code = code;
        }

        boolean matches(Locale locale) {
            return code == null || code.equals(locale.getLanguage());
        }
    }

    @Autowired
    private CurrentAuthentication currentAuthentication;

    @ViewComponent
    private Anchor webinarBannerLink;

    private Domain domain = Domain.ANY;
    private Language language = Language.ANY;
    private Set<Language> supportedLanguages = Set.of();
    private Set<Language> excludedLanguages = Set.of();

    /**
     * @return the page the banner opens, or {@code null} if none is set and the banner is not clickable
     */
    @Nullable
    public String getUrl() {
        String href = webinarBannerLink.getHref();
        return href == null || href.isEmpty() ? null : href;
    }

    public void setUrl(String url) {
        webinarBannerLink.setHref(url);
    }

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
        updateVisibility();
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
        updateVisibility();
    }

    public Set<Language> getSupportedLanguages() {
        return supportedLanguages;
    }

    public void setSupportedLanguages(Set<Language> supportedLanguages) {
        this.supportedLanguages = Set.copyOf(supportedLanguages);
        updateVisibility();
    }

    public Set<Language> getExcludedLanguages() {
        return excludedLanguages;
    }

    public void setExcludedLanguages(Set<Language> excludedLanguages) {
        this.excludedLanguages = Set.copyOf(excludedLanguages);
        updateVisibility();
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        // the descriptor has no attribute for rel
        webinarBannerLink.getElement().setAttribute("rel", "noopener");
        updateVisibility();
    }

    @Subscribe("webinarBannerCloseButton")
    public void onWebinarBannerCloseButtonClick(final ClickEvent<JmixButton> event) {
        setVisible(false);
    }

    // setters may run before or after ReadyEvent (XML properties are applied in an init task), so both re-evaluate
    private void updateVisibility() {
        Locale locale = currentAuthentication.getLocale();
        setVisible(domain.matches(SiteDomainUtils.getCurrentRequestHost())
                && language.matches(locale)
                && (supportedLanguages.isEmpty() || supportedLanguages.stream().anyMatch(value -> value.matches(locale)))
                && excludedLanguages.stream().noneMatch(value -> value.matches(locale)));
    }
}
