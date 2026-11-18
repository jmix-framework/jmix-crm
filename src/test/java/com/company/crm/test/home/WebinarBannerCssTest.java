package com.company.crm.test.home;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The banner artwork is referenced only from CSS, so a moved or renamed image fails silently in the browser.
 */
class WebinarBannerCssTest {

    private static final String THEME_DIR = "META-INF/resources/themes/aura/";
    private static final String CSS = THEME_DIR + "view/webinar.css";
    private static final Pattern URL = Pattern.compile("url\\((['\"])(.+?)\\1\\)");

    @Test
    void everyArtworkUrlResolvesToAnExistingImage() throws IOException {
        List<String> urls = URL.matcher(readCss()).results()
                .map(match -> match.group(2))
                .toList();

        assertThat(urls).isNotEmpty();
        for (String url : urls) {
            // the stylesheet lives in themes/aura/view, so its urls are relative to that folder
            assertThat(url).startsWith("../");
            String resource = THEME_DIR + url.substring("../".length());
            assertThat(getClass().getClassLoader().getResource(resource)).as(url).isNotNull();
        }
    }

    private String readCss() throws IOException {
        try (InputStream css = getClass().getClassLoader().getResourceAsStream(CSS)) {
            assertThat(css).as(CSS).isNotNull();
            return new String(css.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
