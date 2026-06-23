package com.company.crm.test.about;

import com.company.crm.AbstractUiTest;
import com.company.crm.view.about.AboutView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.testassist.UiTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI tests for the About view.
 */
public class AboutViewUiTest extends AbstractUiTest {

    @Autowired
    private ViewNavigators viewNavigators;

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

        assertThat(hrefsOf(community)).containsExactly(
                "https://www.jmix.io/",
                "https://github.com/jmix-framework/jmix",
                "https://forum.jmix.io/",
                "https://www.jmix.io/blog/"
        );
        assertThat(hrefsOf(learn)).containsExactly(
                "https://docs.jmix.io/jmix/intro.html",
                "https://docs.jmix.io/jmix/concepts/index.html",
                "https://ai-assistant.jmix.io/",
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
