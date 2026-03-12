package com.company.crm.app.util.ui;

import com.company.crm.app.ui.component.GridEmptyStateComponent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.data.selection.MultiSelect;
import com.vaadin.flow.data.selection.SingleSelect;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.kit.component.model.shared.Color;
import io.jmix.core.Messages;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.action.inputdialog.InputDialogAction;
import io.jmix.flowui.app.inputdialog.InputParameter;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.grid.DataGridColumn;
import io.jmix.flowui.component.multiselectcombobox.JmixMultiSelectComboBox;
import io.jmix.flowui.fragment.FragmentUtils;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.kit.action.ActionVariant;
import io.jmix.flowui.view.StandardOutcome;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.company.crm.model.datatype.PriceDataType.getCurrencySymbol;
import static io.jmix.flowui.component.UiComponentUtils.getCurrentView;

public final class CrmUiUtils {

    public static final String BADGE_THEME_NAME = "badge";
    public static final String CONTRAST_BADGE = "contrast";
    public static final String DEFAULT_BADGE = "default";
    public static final String SUCCESS_BADGE = "success";
    public static final String WARNING_BADGE = "warning";
    public static final String ERROR_BADGE = "error";
    public static final List<String> BADGE_VARIANTS = Arrays.asList(CONTRAST_BADGE, DEFAULT_BADGE, SUCCESS_BADGE, WARNING_BADGE, ERROR_BADGE);

    public static SvgIcon appLogo() {
        return new SvgIcon("images/logo.svg");
    }

    public static void setDefaultEmptyStateComponent(Grid<?> grid) {
        var text = Instantiator.get(UI.getCurrent()).getOrCreate(Messages.class).getMessage("defaultGridEmptyStateText");
        var emptyState = new GridEmptyStateComponent(text);
        grid.setEmptyStateComponent(emptyState);
    }

    public static void showEmailSendingDialog(Collection<String> emails, boolean allowCustomValues) {
        Instantiator instantiator = Instantiator.get(UI.getCurrent());
        Dialogs dialogs = instantiator.getOrCreate(Dialogs.class);
        Messages messages = instantiator.getOrCreate(Messages.class);
        UiComponents uiComponents = instantiator.getOrCreate(UiComponents.class);

        EmailValidator emailValidator = new EmailValidator(messages.getMessage("invalidEmail"));

        dialogs.createInputDialog(getCurrentView())
                .withHeader(messages.getMessage("sendEmailDialog.header"))
                .withLabelsPosition(Dialogs.InputDialogBuilder.LabelsPosition.TOP)
                .withParameters(
                        InputParameter.parameter("emails")
                                .withRequired(true)
                                .withLabel(messages.getMessage("email"))
                                .withField(() -> {
                                    @SuppressWarnings("unchecked")
                                    JmixMultiSelectComboBox<String> field = uiComponents.create(JmixMultiSelectComboBox.class);
                                    field.setPlaceholder("receiver@mail.com");
                                    if (allowCustomValues) {
                                        field.setAllowCustomValue(true);
                                        field.addCustomValueSetListener(e -> {
                                            String newValue = e.getDetail();
                                            if (emailValidator.apply(newValue, null).isError()) {
                                                return;
                                            }
                                            var currentValues = field.getOptionalValue().orElse(new HashSet<>());
                                            currentValues.add(newValue);
                                            field.setValue(currentValues);
                                        });
                                    }
                                    field.setRequired(true);
                                    field.setWidthFull();
                                    field.setItems(emails);
                                    return field;
                                }))
                .withActions(
                        InputDialogAction.action("sendEmail")
                                .withText(messages.getMessage("send"))
                                .withIcon(VaadinIcon.MAILBOX)
                                .withVariant(ActionVariant.SUCCESS)
                                .withHandler(CrmUiUtils::onSendEmail),

                        InputDialogAction.action("close")
                                .withText(messages.getMessage("actions.Close"))
                                .withIcon(VaadinIcon.CLOSE)
                                .withHandler(CrmUiUtils::closeEmailDialog))
                .build()
                .open();
    }

    public static Popover searchHintPopover() {
        Messages messages = Instantiator.get(UI.getCurrent()).getOrCreate(Messages.class);
        return new Popover(new Html(messages.getMessage("search.hint")));
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

        if (target instanceof HasValue<?, ?> hasValue) {
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

    public static void setBackgroundTransparent(HasStyle component) {
        component.addClassNames(LumoUtility.Background.TRANSPARENT);
    }

    public static void openLink(String link) {
        getCurrentUI().ifPresent(ui -> ui.getPage().open(link, "_blank"));
    }

    public static void setBadge(Span span, @Nullable String badgeVariant) {
        ThemeList themeList = span.getElement().getThemeList();
        themeList.removeIf(theme -> Objects.equals(BADGE_THEME_NAME, theme) || BADGE_VARIANTS.contains(badgeVariant));

        if (StringUtils.isNotBlank(badgeVariant)) {
            themeList.add(BADGE_THEME_NAME);
            themeList.add(badgeVariant);
        }
    }

    public static void setClickableCursor(HasStyle hasStyle) {
        hasStyle.addClassNames("clickable");
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
                Grid.Column<T> column = e.getColumn();
                if (column == null) {
                    return;
                }

                String columnKey = column.getKey();
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

    private static void onSendEmail(ActionPerformedEvent e) {
        Instantiator instantiator = Instantiator.get(UI.getCurrent());
        Messages messages = instantiator.getOrCreate(Messages.class);
        Notifications notifications = instantiator.getOrCreate(Notifications.class);

        Collection<String> emails = ((InputDialogAction) e.getSource()).getInputDialog().getValue("emails");
        String msg = messages.formatMessage("emailSentNotification", String.join(", ", emails));

        notifications.create(msg).withType(Notifications.Type.SYSTEM).show();
        closeEmailDialog(e);
    }

    @SuppressWarnings("DataFlowIssue")
    private static void closeEmailDialog(ActionPerformedEvent e) {
        ((InputDialogAction) e.getSource()).getInputDialog().close(StandardOutcome.CLOSE);
    }

    private CrmUiUtils() {
    }
}
