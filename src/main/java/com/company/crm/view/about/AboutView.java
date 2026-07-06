package com.company.crm.view.about;

import com.company.crm.app.icons.CrmIcons;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServletRequest;
import io.jmix.core.Messages;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.MessageBundle;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.company.crm.view.about.AboutView.ROUTE;

@Route(value = ROUTE, layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.ABOUT)
@ViewDescriptor(path = "about-view.xml")
public class AboutView extends StandardView {

    public static final String ROUTE = "about";

    private static final Logger log = LoggerFactory.getLogger(AboutView.class);

    @Autowired
    private Environment environment;
    @Autowired
    private Notifications notifications;
    @Autowired
    private ObjectProvider<BuildProperties> buildPropertiesProvider;
    @Autowired
    private Messages messages;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    @ViewComponent
    private Span versionValue;
    @ViewComponent
    private Span buildValue;
    @ViewComponent
    private JmixButton copyButton;
    @ViewComponent
    private VerticalLayout communityLinks;
    @ViewComponent
    private VerticalLayout learnLinks;
    @ViewComponent
    private HorizontalLayout socialLinks;
    @ViewComponent
    private Div aboutCrmSource;
    @ViewComponent
    private MessageBundle messageBundle;

    private String deploymentHost;

    @Subscribe
    public void onInit(final InitEvent event) {
        initProductVersion();
        initLinks();
        initAboutCrmSource();
        initCopyButton();
    }

    private void initProductVersion() {
        BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        versionValue.setText(buildProperties != null ? buildProperties.getVersion() : VERSION_FALLBACK);
        buildValue.setText(resolveBuild());
    }

    private String resolveBuild() {
        String[] activeProfiles = environment.getActiveProfiles();
        return activeProfiles.length > 0
                ? String.join(", ", activeProfiles)
                : CrmConstants.SpringProfiles.LOCAL;
    }

    private void initLinks() {
        deploymentHost = currentHost();
        linkIds(COMMUNITY_ITEMS).forEach(id -> addLink(communityLinks, createExternalLink(id)));
        linkIds(LEARN_ITEMS).forEach(id -> addLink(learnLinks, createExternalLink(id)));
        linkIds(SOCIAL_ITEMS).forEach(id -> addLink(socialLinks, createSocialLink(id)));
    }

    private void addLink(HasComponents container, @Nullable Anchor anchor) {
        if (anchor != null) {
            container.add(anchor);
        }
    }

    private List<String> linkIds(String itemsKey) {
        String csv = resolveMeta(itemsKey);
        if (csv == null || csv.isBlank()) {
            log.warn("No link items configured for '{}'", itemsKey);
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(id -> !isHidden(id))
                .toList();
    }

    private void initAboutCrmSource() {
        aboutCrmSource.add(new Text(messageBundle.getMessage("aboutCrm.source") + " "));
        aboutCrmSource.add(createInlineLink(messageBundle.getMessage("aboutCrm.github"), GITHUB_URL));
        aboutCrmSource.add(new Text("."));
    }

    private void initCopyButton() {
        copyButton.setIcon(VaadinIcon.COPY.create());
        copyButton.setTooltipText(messageBundle.getMessage("productVersion.copyTooltip"));
        copyButton.addClickListener(event -> copyProductInfo());
    }

    private void copyProductInfo() {
        String info = messageBundle.getMessage("productVersion.product") + ": "
                + messageBundle.getMessage("product.name") + "\n"
                + messageBundle.getMessage("productVersion.version") + ": " + versionValue.getText() + "\n"
                + messageBundle.getMessage("productVersion.build") + ": " + buildValue.getText();

        UiComponentUtils.copyToClipboard(info);

        notifications.create(messageBundle.getMessage("productVersion.copied"))
                .withType(Notifications.Type.SUCCESS)
                .show();
    }

    @Nullable
    private Anchor createExternalLink(String id) {
        String url = resolveMeta(id + URL_SUFFIX);
        if (url == null || url.isBlank()) {
            log.warn("Skipping link '{}': no '{}{}' configured", id, id, URL_SUFFIX);
            return null;
        }
        Anchor anchor = createInlineLink(messageBundle.getMessage(id), url);
        anchor.addClassName("about-link");

        Icon icon = VaadinIcon.EXTERNAL_LINK.create();
        icon.addClassName("about-link-icon");
        anchor.add(icon);
        return anchor;
    }

    @Nullable
    private Anchor createSocialLink(String id) {
        String url = resolveMeta(id + URL_SUFFIX);
        if (url == null || url.isBlank()) {
            log.warn("Skipping social link '{}': no '{}{}' configured", id, id, URL_SUFFIX);
            return null;
        }
        Anchor anchor = new Anchor();
        anchor.setHref(url);
        anchor.setTarget(AnchorTarget.BLANK);
        anchor.getElement().setAttribute("rel", "noopener");
        anchor.addClassName("about-social-link");

        String title = messageBundle.getMessage(id);
        anchor.getElement().setAttribute("title", title);
        anchor.getElement().setAttribute("aria-label", title);

        Icon icon = resolveIcon(id).create();
        icon.addClassName("about-social-icon");
        anchor.add(icon);
        return anchor;
    }

    private CrmIcons resolveIcon(String id) {
        String name = resolveMeta(id + ICON_SUFFIX);
        if (name != null) {
            try {
                return CrmIcons.valueOf(name.trim().toUpperCase(Locale.ENGLISH));
            } catch (IllegalArgumentException e) {
                log.warn("Unknown CrmIcons '{}' for social link '{}'", name, id);
            }
        }
        return CrmIcons.SPARKLES;
    }

    private Anchor createInlineLink(String text, String url) {
        Anchor anchor = new Anchor(url, text);
        anchor.setTarget(AnchorTarget.BLANK);
        anchor.getElement().setAttribute("rel", "noopener");
        return anchor;
    }

    @Nullable
    private String resolveMeta(String key) {
        Locale locale = currentAuthentication.getLocale();
        String messageGroup = messageBundle.getMessageGroup();
        String value = messages.findMessage(messageGroup, key, locale);
        if (value == null && !DEFAULT_LOCALE.equals(locale)) {
            value = messages.findMessage(messageGroup, key, DEFAULT_LOCALE);
        }
        return value;
    }

    private boolean isHidden(String id) {
        return isHiddenOnTld(deploymentHost, resolveMeta(id + HIDDEN_TLDS_SUFFIX));
    }

    @Nullable
    private String currentHost() {
        VaadinServletRequest request = VaadinServletRequest.getCurrent();
        if (request == null) {
            return null;
        }

        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (forwardedHost != null && !forwardedHost.isBlank()) {
            return forwardedHost.split(",")[0].trim();
        }

        return request.getServerName();
    }

    public static boolean isHiddenOnTld(@Nullable String host, @Nullable String hiddenTldsCsv) {
        if (host == null || hiddenTldsCsv == null || hiddenTldsCsv.isBlank()) {
            return false;
        }
        String tld = tldOf(host);
        if (tld == null) {
            return false;
        }
        return Arrays.stream(hiddenTldsCsv.split(","))
                .map(zone -> zone.trim().toLowerCase(Locale.ENGLISH))
                .anyMatch(tld::equals);
    }

    @Nullable
    private static String tldOf(String host) {
        String h = host.trim().toLowerCase(Locale.ENGLISH);
        int colon = h.indexOf(':');
        if (colon >= 0) {
            h = h.substring(0, colon);
        }
        int dot = h.lastIndexOf('.');
        return (dot >= 0 && dot < h.length() - 1) ? h.substring(dot + 1) : null;
    }

    private static final String GITHUB_URL = "https://github.com/jmix-framework/jmix-crm";

    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private static final String VERSION_FALLBACK = "1.0.0-SNAPSHOT";
    private static final String COMMUNITY_ITEMS = "links.section.project.items";
    private static final String LEARN_ITEMS = "links.section.learn.items";
    private static final String SOCIAL_ITEMS = "links.section.social.items";
    private static final String URL_SUFFIX = ".url";
    private static final String ICON_SUFFIX = ".icon";
    private static final String HIDDEN_TLDS_SUFFIX = ".hiddenTlds";
}
