package com.company.crm.view.about;

import com.company.crm.app.icons.CrmIcons;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.view.main.MainView;
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
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.MessageBundle;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;

import java.util.List;

import static com.company.crm.view.about.AboutView.ROUTE;

@Route(value = ROUTE, layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.ABOUT)
@ViewDescriptor(path = "about-view.xml")
public class AboutView extends StandardView {

    public static final String ROUTE = "about";

    @Autowired
    private Environment environment;
    @Autowired
    private Notifications notifications;
    @Autowired
    private ObjectProvider<BuildProperties> buildPropertiesProvider;

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
        COMMUNITY_LINKS.forEach(link -> communityLinks.add(createExternalLink(link)));
        LEARN_LINKS.forEach(link -> learnLinks.add(createExternalLink(link)));
        SOCIAL_LINKS.forEach(link -> socialLinks.add(createSocialLink(link)));
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

    private Anchor createExternalLink(ExternalLink link) {
        Anchor anchor = createInlineLink(messageBundle.getMessage(link.titleKey()), link.url());
        anchor.addClassName("about-link");

        Icon icon = VaadinIcon.EXTERNAL_LINK.create();
        icon.addClassName("about-link-icon");
        anchor.add(icon);
        return anchor;
    }

    private Anchor createSocialLink(SocialLink link) {
        Anchor anchor = new Anchor();
        anchor.setHref(link.url());
        anchor.setTarget(AnchorTarget.BLANK);
        anchor.getElement().setAttribute("rel", "noopener");
        anchor.addClassName("about-social-link");

        String title = messageBundle.getMessage(link.titleKey());
        anchor.getElement().setAttribute("title", title);
        anchor.getElement().setAttribute("aria-label", title);

        Icon icon = link.icon().create();
        icon.addClassName("about-social-icon");
        anchor.add(icon);
        return anchor;
    }

    private Anchor createInlineLink(String text, String url) {
        Anchor anchor = new Anchor(url, text);
        anchor.setTarget(AnchorTarget.BLANK);
        anchor.getElement().setAttribute("rel", "noopener");
        return anchor;
    }

    private record ExternalLink(String titleKey, String url) {
    }

    private record SocialLink(String titleKey, String url, CrmIcons icon) {
    }

    private static final String VERSION_FALLBACK = "1.0.0-SNAPSHOT";

    private static final String GITHUB_URL = "https://github.com/jmix-framework/jmix";

    private static final List<ExternalLink> COMMUNITY_LINKS = List.of(
            new ExternalLink("links.website", "https://www.jmix.io/"),
            new ExternalLink("links.github", "https://github.com/jmix-framework/jmix"),
            new ExternalLink("links.forum", "https://forum.jmix.io/"),
            new ExternalLink("resources.blog", "https://www.jmix.io/blog/")
    );

    private static final List<ExternalLink> LEARN_LINKS = List.of(
            new ExternalLink("resources.aiAssistant", "https://ai-assistant.jmix.io/"),
            new ExternalLink("links.documentation", "https://docs.jmix.io/jmix/intro.html"),
            new ExternalLink("resources.concepts", "https://docs.jmix.io/jmix/concepts/index.html"),
            new ExternalLink("resources.introCourse", "https://www.udemy.com/course/rapid-application-development-with-jmix/"),
            new ExternalLink("resources.uiCourse", "https://www.udemy.com/course/frontend-with-java-and-jmix/?referralCode=FCE0EA245FF7F448C414")
    );

    private static final List<SocialLink> SOCIAL_LINKS = List.of(
            new SocialLink("links.youtube", "https://www.youtube.com/channel/UCEmWc8OwhgHnAV7vVVxtglQ", CrmIcons.YOUTUBE),
            new SocialLink("links.linkedin", "https://www.linkedin.com/company/jmix-platform/", CrmIcons.LINKEDIN),
            new SocialLink("links.facebook", "https://www.facebook.com/JmixPlatform", CrmIcons.FACEBOOK),
            new SocialLink("links.twitter", "https://twitter.com/JmixPlatform", CrmIcons.X)
    );
}
