package com.company.crm.app.util.ui.listener.resize;

import com.company.crm.app.util.ui.UiUtils;
import com.vaadin.flow.shared.Registration;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.View;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@FunctionalInterface
public interface WidthResizeListener {

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
        UiUtils.executeOnClientWidthAsync(this::configureUiForWidth);
    }

    default Optional<CompletableFuture<Integer>> onClientWidthFuture() {
        return UiUtils.onClientWidthFuture();
    }

    default void onClientWidthFuture(Consumer<Integer> onWidth) {
        UiUtils.onClientWidthFuture(onWidth);
    }

    default Optional<Registration> addAsyncWidthResizeListener() {
        return UiUtils.addBrowserWindowResizeListener((l) -> configureComponentsForWidthAsync());
    }
}
