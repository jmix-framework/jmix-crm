package com.company.crm.app.util.ui;

import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.order.Order;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.page.BrowserWindowResizeEvent;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.data.selection.MultiSelect;
import com.vaadin.flow.data.selection.SingleSelect;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.shared.Registration;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.grid.DataGridColumn;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.company.crm.model.datatype.PriceDataType.getCurrencySuffix;
import static com.company.crm.model.datatype.PriceDataType.getCurrencySymbol;

public final class CrmUiUtils {

    private static final String GET_CLIENT_WIDTH_FUNC = "return window.innerWidth";
    private static final String GET_CLIENT_HEIGHT_FUNC = "return window.innerHeight";

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

    public static void setCursorPointer(HasStyle hasStyle) {
        setCursor(hasStyle, "pointer");
    }

    public static void setCursor(HasStyle hasStyle, String cursor) {
        hasStyle.getStyle().set("cursor", cursor);
    }

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

    public static void addColumnHeaderCurrencySuffix(DataGrid<?> grid, String...columnKey) {
        for (String key : columnKey) {
            DataGridColumn<?> column = grid.getColumnByKey(key);
            if (column != null) {
                column.setHeader(column.getHeaderText() + ", " + getCurrencySymbol());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> void addRowSelectionInMultiSelectMode(DataGrid<T> grid, String...ignoredColumn) {
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
