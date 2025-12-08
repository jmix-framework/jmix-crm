package com.company.crm.app.util.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.BrowserWindowResizeEvent;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.shared.Registration;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class UiUtils {

    private static final String GET_CLIENT_WIDTH_FUNC = "return window.innerWidth";
    private static final String GET_CLIENT_HEIGHT_FUNC = "return window.innerHeight";

    public static Optional<UI> getCurrentUI() {
        return Optional.ofNullable(UI.getCurrent());
    }

    public static Optional<Page> getCurrentPage() {
        return getCurrentUI().map(UI::getPage);
    }

    public static void reloadCurrentPage() {
        getCurrentPage().ifPresent(Page::reload);
    }

    public static Optional<Registration> addBrowserWindowResizeListener(Consumer<BrowserWindowResizeEvent> onResize) {
        return getCurrentPage().map(page -> page.addBrowserWindowResizeListener(onResize::accept));
    }

    public static Optional<PendingJavaScriptResult> executeJs(final String js) {
        return getCurrentPage().map(page -> page.executeJs(js));
    }

    public static void executeOnClientWidthAsync(Consumer<Integer> onWidth) {
        executeGetClientWidthJs().ifPresent(p -> p.then(Integer.class, onWidth::accept));
    }

    public static Optional<CompletableFuture<Integer>> onClientWidthFuture() {
        return executeGetClientWidthJs().map(p -> p.toCompletableFuture(Integer.class));
    }

    public static void onClientWidthFuture(Consumer<Integer> onWidth) {
        onClientWidthFuture().ifPresent(c -> c.whenComplete((width, t) -> onWidth.accept(width)));
    }

    public static void executeOnClientHeightAsync(Consumer<Integer> onHeight) {
        executeGetClientHeightJs().ifPresent(p -> p.then(Integer.class, onHeight::accept));
    }

    public static Optional<CompletableFuture<Integer>> onClientHeightFuture() {
        return executeGetClientHeightJs().map(p -> p.toCompletableFuture(Integer.class));
    }

    public Optional<CompletableFuture<Integer>> onClientHeightFuture(Consumer<Integer> onHeight) {
        return onClientHeightFuture().map(c -> c.whenComplete((width, t) -> onHeight.accept(width)));
    }

    private static Optional<PendingJavaScriptResult> executeGetClientWidthJs() {
        return executeJs(GET_CLIENT_WIDTH_FUNC);
    }

    private static Optional<PendingJavaScriptResult> executeGetClientHeightJs() {
        return executeJs(GET_CLIENT_HEIGHT_FUNC);
    }

    private UiUtils() {
    }
}
