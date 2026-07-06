package com.company.crm.test.about;

import com.company.crm.AbstractUiTest;
import com.company.crm.view.about.AboutView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.core.Messages;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.testassist.UiTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

public class AboutViewUiTest extends AbstractUiTest {

    private static final String ABOUT_GROUP = "com.company.crm.view.about";

    @Autowired
    private ViewNavigators viewNavigators;
    @Autowired
    private Messages messages;

    @Test
    void aboutViewShowsProductVersionInfo() {
        AboutView view = navigateToAbout();

        Span versionValue = UiTestUtils.getComponent(view, "versionValue");
        Span buildValue = UiTestUtils.getComponent(view, "buildValue");

        assertThat(versionValue.getText()).isNotBlank();
        assertThat(buildValue.getText()).isNotBlank();
    }

    @Test
    void aboutViewRendersCommunityAndLearningLinks() {
        AboutView view = navigateToAbout();

        VerticalLayout community = UiTestUtils.getComponent(view, "communityLinks");
        VerticalLayout learn = UiTestUtils.getComponent(view, "learnLinks");

        String utm = "?utm_source=aboutb2b&utm_medium=aboutb2b&utm_campaign=b2bcrm";
        assertThat(hrefsOf(community)).containsExactly(
                "https://www.jmix.io/" + utm,
                "https://github.com/jmix-framework/jmix",
                "https://forum.jmix.io/" + utm,
                "https://www.jmix.io/blog/" + utm
        );
        assertThat(hrefsOf(learn)).containsExactly(
                "https://ai-assistant.jmix.io/" + utm,
                "https://docs.jmix.io/jmix/intro.html" + utm,
                "https://docs.jmix.io/jmix/concepts/index.html" + utm,
                "https://www.udemy.com/course/rapid-application-development-with-jmix/",
                "https://www.udemy.com/course/frontend-with-java-and-jmix/?referralCode=FCE0EA245FF7F448C414"
        );

        anchorsOf(community).forEach(anchor -> assertThat(anchor.getTarget()).contains("_blank"));
        anchorsOf(learn).forEach(anchor -> assertThat(anchor.getTarget()).contains("_blank"));
    }

    @Test
    void aboutViewRendersSocialLinksAsIconsOnly() {
        AboutView view = navigateToAbout();

        HorizontalLayout social = UiTestUtils.getComponent(view, "socialLinks");
        List<Anchor> links = anchorsOf(social);

        assertThat(hrefsOf(social)).containsExactly(
                "https://www.youtube.com/channel/UCEmWc8OwhgHnAV7vVVxtglQ",
                "https://www.linkedin.com/company/jmix-platform/",
                "https://www.facebook.com/JmixPlatform",
                "https://twitter.com/JmixPlatform"
        );

        // Icon-only: no visible text, opens in a new tab, exposes an accessible label.
        assertThat(links).allSatisfy(anchor -> {
            assertThat(anchor.getText()).isBlank();
            assertThat(anchor.getTarget()).contains("_blank");
            assertThat(anchor.getElement().getAttribute("aria-label")).isNotBlank();
        });
    }

    @Test
    void localeOverridesLinksAndSocialSet() {
        Locale ru = Locale.forLanguageTag("ru");

        // Community/learning links point at jmix.ru with campaign params.
        assertThat(messages.findMessage(ABOUT_GROUP, "links.website.url", ru))
                .isEqualTo("https://www.jmix.ru/?utm_source=aboutb2b&utm_medium=aboutb2b&utm_campaign=b2bcrm");
        assertThat(messages.findMessage(ABOUT_GROUP, "resources.introCourse.url", ru))
                .isEqualTo("https://stepik.org/course/190140/promo");
        // Russian drops the (Udemy) UI course from the learning section.
        assertThat(messages.findMessage(ABOUT_GROUP, "links.section.learn.items", ru))
                .doesNotContain("uiCourse");

        // Russian uses a different set of social networks with their own icons.
        assertThat(messages.findMessage(ABOUT_GROUP, "links.section.social.items", ru))
                .isEqualTo("links.vk,links.telegram,links.max,links.youtube");
        assertThat(messages.findMessage(ABOUT_GROUP, "links.vk.url", ru))
                .isEqualTo("https://vk.com/jmixplatform");
        assertThat(messages.findMessage(ABOUT_GROUP, "links.max.icon", ru)).isEqualTo("MAX");

        // Keys not overridden by Russian inherit the English defaults.
        assertThat(messages.findMessage(ABOUT_GROUP, "links.youtube.icon", Locale.ENGLISH))
                .isEqualTo("YOUTUBE");
    }

    @Test
    void hiddenWhenDeploymentTldMatches() {
        assertThat(AboutView.isHiddenOnTld("crm.jmix.ru", "ru")).isTrue();
        assertThat(AboutView.isHiddenOnTld("jmix.ru:8080", "ru")).isTrue();   // port stripped
        assertThat(AboutView.isHiddenOnTld("CRM.JMIX.RU", "ru")).isTrue();    // case-insensitive
        assertThat(AboutView.isHiddenOnTld("site.by", "ru, by, kz")).isTrue(); // multi-zone list
    }

    @Test
    void visibleWhenTldDiffersOrRuleAbsent() {
        assertThat(AboutView.isHiddenOnTld("crm.jmix.io", "ru")).isFalse();
        assertThat(AboutView.isHiddenOnTld("localhost", "ru")).isFalse();     // no TLD
        assertThat(AboutView.isHiddenOnTld("crm.jmix.ru", null)).isFalse();   // no rule
        assertThat(AboutView.isHiddenOnTld("crm.jmix.ru", "  ")).isFalse();
        assertThat(AboutView.isHiddenOnTld(null, "ru")).isFalse();            // no host (e.g. no request)
    }

    private AboutView navigateToAbout() {
        viewNavigators.view(UiTestUtils.getCurrentView(), AboutView.class).navigate();
        return UiTestUtils.getCurrentView();
    }

    private List<Anchor> anchorsOf(Component container) {
        return container.getChildren()
                .filter(Anchor.class::isInstance)
                .map(Anchor.class::cast)
                .toList();
    }

    private List<String> hrefsOf(Component container) {
        return anchorsOf(container).stream()
                .map(Anchor::getHref)
                .toList();
    }
}
