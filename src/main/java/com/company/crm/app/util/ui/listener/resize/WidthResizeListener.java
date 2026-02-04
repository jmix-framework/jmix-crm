package com.company.crm.app.util.ui.listener.resize;

import com.company.crm.app.util.ui.CrmUiUtils;
import com.vaadin.flow.shared.Registration;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.View;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@FunctionalInterface
public interface WidthResizeListener {

    static boolean isWidthChanged(int actualWidth, int lastWidth, int breakpoint) {
        return lastWidth < 0
                || (actualWidth >= breakpoint && lastWidth < breakpoint)
                || (actualWidth <= breakpoint && lastWidth > breakpoint);
    }

    void configureUiForWidth(int width);

    @Subscribe
    default void initWidthResizeListener(View.InitEvent event) {
        View<?> view = event.getSource();
        view.addAttachListener(e -> {
            if (e.isInitialAttach()) {
                initWidthResizeListener(view);
            }
        });
    }

    default void initWidthResizeListener(View<?> listenerOwner) {
        configureComponentsForWidthAsync();
        addAsyncWidthResizeListener().ifPresent(registration ->
                listenerOwner.addDetachListener(e -> {
                    try {
                        registration.remove();
                    } catch (Throwable ignored) {
                    }
                }));
    }

    default void configureComponentsForWidthAsync() {
        CrmUiUtils.executeOnClientWidthAsync(this::configureUiForWidth);
    }

    default Optional<CompletableFuture<Integer>> onClientWidthFuture() {
        return CrmUiUtils.onClientWidthFuture();
    }

    default void onClientWidthFuture(Consumer<Integer> onWidth) {
        CrmUiUtils.onClientWidthFuture(onWidth);
    }

    default Optional<Registration> addAsyncWidthResizeListener() {
        return CrmUiUtils.addBrowserWindowResizeListener((l) -> configureComponentsForWidthAsync());
    }
}
