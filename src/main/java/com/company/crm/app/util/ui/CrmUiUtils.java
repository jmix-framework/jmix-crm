package com.company.crm.app.util.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.BrowserWindowResizeEvent;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.data.selection.MultiSelect;
import com.vaadin.flow.data.selection.SingleSelect;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.shared.Registration;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.kit.component.model.shared.Color;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.grid.DataGridColumn;
import io.jmix.flowui.fragment.FragmentUtils;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.company.crm.model.datatype.PriceDataType.getCurrencySymbol;

public final class CrmUiUtils {

    public static final String CONTRAST_BADGE = "contrast";
    public static final String DEFAULT_BADGE = "default";
    public static final String SUCCESS_BADGE = "success";
    public static final String WARNING_BADGE = "warning";
    public static final String ERROR_BADGE = "error";

    private static final String GET_CLIENT_WIDTH_FUNC = "return window.innerWidth";
    private static final String GET_CLIENT_HEIGHT_FUNC = "return window.innerHeight";

    public static Popover searchHintPopover() {
        return new Popover(new Html("<p>Press <b>Enter</b> to apply filter</p>"));
    }

    public static Popover setSearchHintPopover(Component target) {
        return setSearchHintPopover(target, true);
    }

    public static Popover setSearchHintPopover(Component target, boolean oneTime) {
        Popover popover = searchHintPopover();
        popover.setTarget(target);
        popover.setOpenOnFocus(true);


        if (oneTime) {
            AtomicReference<Runnable> detachRunnable = new AtomicReference<>(null);
            popover.addOpenedChangeListener(e -> {
                if (e.isOpened()) {
                    detachRunnable.set(popover::removeFromParent);
                } else {
                    Runnable detach = detachRunnable.get();
                    if (detach != null) {
                        detach.run();
                    }
                }
            });
        }

        if (target instanceof HasValue<?,?> hasValue) {
            hasValue.addValueChangeListener(e -> popover.close());
        }

        return popover;
    }

    public static Optional<String> getComponentId(Component component) {
        return component.getId().or(() -> FragmentUtils.getComponentId(component));
    }

    public static void setBackgroundTransparent(Chart chart) {
        chart.setBackgroundColor(new Color("rgba(255, 255, 255, 0)"));
    }

    public static void setBackgroundTransparent(Component component) {
        component.getStyle().setBackground("transparent");
    }

    public static void openLink(String link) {
        getCurrentUI().ifPresent(ui -> ui.getPage().open(link, "_blank"));
    }

    public static <T extends HasStyle> T makeResizable(T hasStyle) {
        return makeResizable(hasStyle, ResizeMode.BOTH);
    }

    public static <T extends HasStyle> T makeResizable(T hasStyle, ResizeMode resizeMode) {
        Style style = hasStyle.getStyle();
        style.setOverflow(Style.Overflow.AUTO);
        switch (resizeMode) {
            case VERTICAL:
                style.set("resize", "vertical");
                break;
            case HORIZONTAL:
                style.set("resize", "horizontal");
                break;
            case BOTH:
                style.set("resize", "both");
                break;
        }
        return hasStyle;
    }

    public enum ResizeMode {
        VERTICAL, HORIZONTAL, BOTH
    }

    public static void setBadge(Span span, String badgeVariant) {
        span.getElement().getThemeList().add("badge " + badgeVariant);
    }

    public static void setCursorPointer(HasStyle hasStyle) {
        setCursor(hasStyle, "pointer");
    }

    public static void setCursor(HasStyle hasStyle, String cursor) {
        hasStyle.getStyle().set("cursor", cursor);
    }

    public static Optional<UI> getCurrentUI() {
        return Optional.ofNullable(UI.getCurrent());
    }

    public static Optional<QueryParameters> getCurrentQueryParameters() {
        return getCurrentUI().map(ui -> ui.getActiveViewLocation().getQueryParameters());
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

    public static void addColumnHeaderCurrencySuffix(DataGrid<?> grid, String... columnKey) {
        for (String key : columnKey) {
            DataGridColumn<?> column = grid.getColumnByKey(key);
            if (column != null) {
                column.setHeader(column.getHeaderText() + ", " + getCurrencySymbol());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> void addRowSelectionInMultiSelectMode(DataGrid<T> grid, String... ignoredColumn) {
        if (grid.isMultiSelect()) {
            grid.addItemClickListener(e -> {
                String columnKey = e.getColumn().getKey();
                if (Arrays.asList(ignoredColumn).contains(columnKey)) {
                    return;
                }

                T item = e.getItem();
                try {
                    MultiSelect<Grid<T>, T> multiSelect = grid.asMultiSelect();
                    if (multiSelect.isSelected(item)) {
                        multiSelect.deselect(item);
                    } else {
                        multiSelect.select(item);
                    }
                } catch (IllegalStateException mayBeNotMultiSelect) {
                    try {
                        SingleSelect<Grid<T>, T> singleSelect = grid.asSingleSelect();
                        singleSelect.setValue(item);
                    } catch (Exception ignored) {
                        throw mayBeNotMultiSelect;
                    }
                }
            });
        }
    }

    private static Optional<PendingJavaScriptResult> executeGetClientWidthJs() {
        return executeJs(GET_CLIENT_WIDTH_FUNC);
    }

    private static Optional<PendingJavaScriptResult> executeGetClientHeightJs() {
        return executeJs(GET_CLIENT_HEIGHT_FUNC);
    }

    private CrmUiUtils() {
    }
}
