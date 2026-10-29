package com.company.crm.test.home;

import com.company.crm.AbstractUiTest;
import com.company.crm.app.util.ui.SiteDomainUtils;
import com.company.crm.view.home.HomeView;
import com.company.crm.view.webinar.WebinarBannerFragment;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.fragment.FragmentUtils;
import io.jmix.flowui.kit.component.button.JmixButton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class HomeViewTest extends AbstractUiTest {

    @Test
    void configuresIndependentWebinarCampaigns() {
        var view = viewTestSupport.navigateTo(HomeView.class);
        WebinarBannerFragment release = viewTestSupport.getComponent(view, "webinarBannerFragment");
        WebinarBannerFragment workshop = viewTestSupport.getComponent(view, "workshopBannerFragment");

        assertThat(release.getDomain()).isEqualTo(WebinarBannerFragment.Domain.JMIX_IO);
        assertThat(release.getExcludedLanguages()).containsExactly(WebinarBannerFragment.Language.RU);
        assertThat(release.getArtwork()).isEqualTo(WebinarBannerFragment.Artwork.RELEASE_3_1);
        assertThat(release.getUrl()).isEqualTo(
                "https://forms.jmix.io/jmix-3.1-release-webinar?utm_source=banner&utm_medium=site&utm_campaign=webinar");

        assertThat(workshop.getDomain()).isEqualTo(WebinarBannerFragment.Domain.JMIX_RU);
        assertThat(workshop.getLanguage()).isEqualTo(WebinarBannerFragment.Language.ANY);
        assertThat(workshop.getSupportedLanguages()).isEmpty();
        assertThat(workshop.getExcludedLanguages()).isEmpty();
        assertThat(workshop.getArtwork()).isEqualTo(WebinarBannerFragment.Artwork.DMS_AI);
        assertThat(workshop.getContent().getElement().getAttribute("data-artwork")).isEqualTo("DMS_AI");
        assertThat(workshop.getUrl()).isEqualTo(
                "https://event.jmix.ru/workshop_dms_ai?utm_source=demo&utm_medium=site&utm_campaign=workshop");

        workshop.setArtwork(WebinarBannerFragment.Artwork.RELEASE_3_1);
        assertThat(workshop.getContent().getElement().getAttribute("data-artwork")).isEqualTo("RELEASE_3_1");
        assertThat(release.getArtwork()).isEqualTo(WebinarBannerFragment.Artwork.RELEASE_3_1);
        assertThat(release.getContent().getElement().getAttribute("data-artwork")).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "jmix.ru, ru, false, true",
            "crm.jmix.ru, en, false, true",
            "crm.jmix.ru, de, false, true",
            "jmix.io, en, true, false",
            "demo.jmix.io, de, true, false",
            "demo.jmix.io, ru-RU, false, false",
            "localhost, ru, false, false",
            "jmix.ru.example.com, ru, false, false",
            "fakejmix.ru, ru, false, false",
            ", en, false, false"
    })
    void selectsBannerByDomainAndLanguage(String host, String language, boolean releaseVisible,
                                         boolean workshopVisible) {
        var view = viewTestSupport.navigateTo(HomeView.class);
        WebinarBannerFragment release = viewTestSupport.getComponent(view, "webinarBannerFragment");
        WebinarBannerFragment workshop = viewTestSupport.getComponent(view, "workshopBannerFragment");

        try (var domains = mockStatic(SiteDomainUtils.class, CALLS_REAL_METHODS)) {
            domains.when(SiteDomainUtils::getCurrentRequestHost).thenReturn(host);
            refreshVisibility(Locale.forLanguageTag(language), release, workshop);

            assertThat(release.isVisible()).isEqualTo(releaseVisible);
            assertThat(workshop.isVisible()).isEqualTo(workshopVisible);
        }
    }

    @Test
    void closingOneCampaignLeavesTheOtherOpen() {
        var view = viewTestSupport.navigateTo(HomeView.class);
        WebinarBannerFragment release = viewTestSupport.getComponent(view, "webinarBannerFragment");
        WebinarBannerFragment workshop = viewTestSupport.getComponent(view, "workshopBannerFragment");

        try (var domains = mockStatic(SiteDomainUtils.class, CALLS_REAL_METHODS)) {
            domains.when(SiteDomainUtils::getCurrentRequestHost).thenReturn("crm.jmix.ru");
            refreshVisibility(Locale.ENGLISH, release, workshop);
            release.setDomain(WebinarBannerFragment.Domain.ANY);
            assertThat(release.isVisible()).isTrue();
            assertThat(workshop.isVisible()).isTrue();

            ((JmixButton) FragmentUtils.getComponent(release, "webinarBannerCloseButton")).click();

            assertThat(release.isVisible()).isFalse();
            assertThat(workshop.isVisible()).isTrue();

            ((JmixButton) FragmentUtils.getComponent(workshop, "webinarBannerCloseButton")).click();
            assertThat(workshop.isVisible()).isFalse();
        }
    }

    private void refreshVisibility(Locale locale, WebinarBannerFragment... banners) {
        CurrentAuthentication authentication = mock(CurrentAuthentication.class);
        when(authentication.getLocale()).thenReturn(locale);
        for (WebinarBannerFragment banner : banners) {
            ReflectionTestUtils.setField(banner, "currentAuthentication", authentication);
            banner.setDomain(banner.getDomain());
        }
    }
}
