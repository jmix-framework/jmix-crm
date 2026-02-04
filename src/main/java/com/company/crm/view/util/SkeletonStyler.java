package com.company.crm.view.util;

import com.vaadin.flow.component.Component;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * Utility for applying/removing a "skeleton" style.
 * This works by adding/removing the `skeleton` token to the component's `theme`.
 * A related CSS rule has the condition `[theme~="skeleton"]`.
 */
public final class SkeletonStyler {

    private static final String THEME_ATTR = "theme";
    private static final String SKELETON = "skeleton";

    private SkeletonStyler() {
    }

    public static void apply(Component component) {
        if (component == null) return;

        var element = component.getElement();
        String theme = element.getAttribute(THEME_ATTR);

        if (StringUtils.isBlank(theme)) {
            element.setAttribute(THEME_ATTR, SKELETON);
        } else if (!hasSkeletonThemeAttribute(theme)) {
            element.setAttribute(THEME_ATTR, theme + " " + SKELETON);
        }
    }

    public static void apply(Component... components) {
        if (components == null) return;
        Arrays.stream(components).filter(Objects::nonNull).forEach(SkeletonStyler::apply);
    }

    public static void apply(Collection<? extends Component> components) {
        if (components == null) return;
        components.stream().filter(Objects::nonNull).forEach(SkeletonStyler::apply);
    }

    public static void remove(Component component) {
        if (component == null) return;

        var element = component.getElement();
        String theme = element.getAttribute(THEME_ATTR);

        if (StringUtils.isBlank(theme)) return;

        String updated = removeSkeletonThemeAttribute(theme);

        if (!Objects.equals(theme, updated)) {
            if (updated.isBlank()) {
                element.removeAttribute(THEME_ATTR);
            } else {
                element.setAttribute(THEME_ATTR, updated);
            }
        }
    }

    public static void remove(Component... components) {
        if (components == null) return;
        Arrays.stream(components)
                .filter(Objects::nonNull)
                .forEach(SkeletonStyler::remove);
    }

    public static void remove(Collection<? extends Component> components) {
        if (components == null) return;
        components.stream()
                .filter(Objects::nonNull)
                .forEach(SkeletonStyler::remove);
    }

    public static Runnable runWithSkeleton(Component component, Runnable task) {
        return () -> {
            apply(component);
            try {
                if (task != null) task.run();
            } finally {
                remove(component);
            }
        };
    }

    private static boolean hasSkeletonThemeAttribute(String themeAttribute) {
        return Arrays.stream(themeAttribute.split("\\s+"))
                .anyMatch(t -> t.equalsIgnoreCase(SKELETON));
    }

    private static String removeSkeletonThemeAttribute(String tokens) {
        return String.join(" ", Arrays.stream(tokens.split("\\s+"))
                .filter(t -> !t.equalsIgnoreCase(SKELETON))
                .toArray(String[]::new)).trim();
    }
}
