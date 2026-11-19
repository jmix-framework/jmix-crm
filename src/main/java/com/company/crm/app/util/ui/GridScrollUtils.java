package com.company.crm.app.util.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewControllerUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.company.crm.app.util.ui.CrmUiUtils.getComponentId;
import static io.jmix.flowui.component.UiComponentUtils.isComponentAttachedToDialog;

/**
 * Remembers the first visible row in the URL, based on the
 * <a href="https://cookbook.vaadin.com/grid-scroll-tracking">Vaadin Cookbook recipe</a>.
 */
public final class GridScrollUtils {

    private static final Logger log = LoggerFactory.getLogger(GridScrollUtils.class);
    private static final int SCROLL_DEBOUNCE_MS = 500;

    private GridScrollUtils() {
    }

    @Nullable
    public static String rememberScrollPosition(DataGrid<?> grid, String ownerId, Component owner) {
        try {
            var id = getComponentId(grid);
            if (id.isEmpty()) {
                return null;
            }

            String scrollParameter = "scroll." + ownerId + ":" + id.get();
            registerScrollListener(grid, scrollParameter);
            if (owner instanceof View<?> view) {
                ViewControllerUtils.addQueryParametersChangeListener(view, event -> event.getQueryParameters()
                        .getSingleParameter(scrollParameter)
                        .ifPresent(index -> {
                            UI ui = UI.getCurrent();
                            if (ui != null) {
                                ui.beforeClientResponse(grid, context -> restoreScrollPosition(grid, index));
                            }
                        }));
            }
            grid.addAttachListener(event -> {
                if (!(owner instanceof View<?>) || isComponentAttachedToDialog(grid)) {
                    restoreFromBrowserUrl(grid, scrollParameter);
                }
            });
            return scrollParameter;
        } catch (RuntimeException e) {
            log.debug("Could not enable grid scroll tracking", e);
            return null;
        }
    }

    private static void registerScrollListener(DataGrid<?> grid, String scrollParameter) {
        grid.getElement().addJsInitializer("""
                const controller = new AbortController();
                let timeout;
                (async () => {
                    try {
                        await this.updateComplete;
                        // shortcut: Cookbook uses Vaadin internals, switch when public equivalents exist
                        const scroller = this.$?.table;
                        if (controller.signal.aborted || !scroller) return;
                        scroller.addEventListener('scroll', () => {
                            const path = location.pathname;
                            clearTimeout(timeout);
                            timeout = setTimeout(async () => {
                                try {
                                    if (!this.isConnected || location.pathname !== path) return;
                                    const index = this._firstVisibleIndex;
                                    if (!Number.isSafeInteger(index) || index < 0) return;
                                    const saved = new URLSearchParams(location.search).get($0) ?? '0';
                                    if (String(index) !== saved) await this.$server.receiveScrollPosition(index, path);
                                } catch {
                                    // Scroll tracking is optional.
                                }
                            }, $1);
                        }, { signal: controller.signal, passive: true });
                    } catch {
                        controller.abort();
                    }
                })();
                return () => {
                    clearTimeout(timeout);
                    controller.abort();
                };
                """, scrollParameter, SCROLL_DEBOUNCE_MS);
    }

    public static void saveScrollPosition(DataGrid<?> grid, @Nullable String scrollParameter, Integer index, String path) {
        if (scrollParameter == null || index == null || index < 0 || path == null || !grid.isAttached()) {
            return;
        }

        try {
            // Merge with the live URL so a filter or pagination change cannot be replaced by a stale snapshot.
            grid.getElement().executeJs("""
                    try {
                        if (!this.isConnected || location.pathname !== $2) return;
                        const url = new URL(location.href);
                        url.searchParams.set($0, String($1));
                        history.replaceState(history.state, '', url);
                    } catch {
                        // The browser can decline a history update.
                    }
                    """, scrollParameter, index, path);
        } catch (RuntimeException e) {
            log.debug("Could not update grid scroll position in the URL", e);
        }
    }

    private static void restoreFromBrowserUrl(DataGrid<?> grid, String scrollParameter) {
        try {
            grid.getElement().executeJs("return new URLSearchParams(location.search).get($0);", scrollParameter)
                    .then(String.class, value -> restoreScrollPosition(grid, value));
        } catch (RuntimeException e) {
            log.debug("Could not read grid scroll position from the URL", e);
        }
    }

    private static void restoreScrollPosition(DataGrid<?> grid, String value) {
        if (value == null || !grid.isAttached()) {
            return;
        }

        try {
            int index = Integer.parseInt(value);
            if (index >= 0) {
                int itemCount = grid.getDataCommunicator().getItemCount();
                if (itemCount > 0) {
                    grid.scrollToIndex(Math.min(index, itemCount - 1));
                }
            }
        } catch (NumberFormatException ignored) {
            // A URL can contain an invalid or out-of-range index.
        } catch (RuntimeException e) {
            log.debug("Could not restore grid scroll position", e);
        }
    }
}
