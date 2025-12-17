package com.company.crm.app.ui.component.card;

import com.company.crm.app.util.date.range.LocalDateRange;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.jmix.core.Messages;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.component.card.JmixCard;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButton;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButtonItem;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButtonVariant;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Function;

import static org.apache.commons.lang3.StringUtils.defaultString;

public class CrmCard extends JmixCard implements ApplicationContextAware {

    private static final CardPeriod DEFAULT_INITIAL_PERIOD = CardPeriod.MONTH;

    private static final DateTimeFormatter DATE_WITHOUT_YEAR = DateTimeFormatter.ofPattern("dd MMM");
    private static final DateTimeFormatter DATE_WITH_YEAR = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private static final String PREVIOUS_RANGE_DELTA_COMPONENT_ID = "delta-value-component";

    private Component title = new Span("");
    private boolean withoutBackground = false;
    private boolean hasPeriodFilter = false;
    private boolean hasEllipsisButton = false;

    private Function<CardPeriod, Component> contentProvider;

    private CardPeriod currentPeriod = DEFAULT_INITIAL_PERIOD;

    private ApplicationContext applicationContext;

    {
        addThemeVariants(CardVariant.LUMO_ELEVATED, CardVariant.LUMO_OUTLINED);
        updateBackground();
    }

    public void fillAsStaticCard(String title,
                                 Component component) {
        fillAsStaticCard(title, 1, component);
    }

    public void fillAsStaticCard(String title,
                                 int colspan,
                                 Component component) {
        fillAsPeriodCard(title, colspan, p -> component);
        withPeriodFilter(false);
    }

    public void fillAsPeriodCard(String title,
                                 Function<CardPeriod, Component> contentProvider) {
        fillAsPeriodCard(null, title, contentProvider);
    }

    public void fillAsPeriodCard(String title,
                                 int colspan,
                                 Function<CardPeriod, Component> contentProvider) {
        fillAsPeriodCard(null, colspan, title, contentProvider);
    }

    public void fillAsPeriodCard(String id,
                                 String title,
                                 Function<CardPeriod, Component> contentProvider) {
        fillAsPeriodCard(id, 1, title, contentProvider);
    }

    public void fillAsPeriodCard(String id,
                                 int colspan,
                                 String title,
                                 Function<CardPeriod, Component> contentProvider) {
        fillAsPeriodCard(id, colspan, createDefaultTitleComponent(title), contentProvider);
    }

    public void fillAsPeriodCard(String id,
                                 int colspan,
                                 Component title,
                                 Function<CardPeriod, Component> contentProvider) {
        this.title = title;
        this.hasPeriodFilter = true;
        this.contentProvider = contentProvider;
        setColspan(colspan);
        if (id != null) {
            setId(id);
        }
        refreshContent();
    }

    public void defaultRangeStatPeriodCard(String title, Function<CardPeriod, RangeStatCardInfo> statInfoProvider) {
        fillAsPeriodCard(title, period -> statInfoProvider.apply(period).createDefaultContent());
    }

    public record RangeStatCardInfo(LocalDateRange range, String currentRangeValue, String previousRangeDelta) {

        public Component createDefaultContent() {
            return defaultContentProvider().apply(this);
        }

        public Function<RangeStatCardInfo, Component> defaultContentProvider() {
            return info -> {
                String currentValue = info.currentRangeValue();
                var contentComponent = new Div(new H1(currentValue));
                contentComponent.setWidthFull();

                var horizontalLayout = new HorizontalLayout();
                horizontalLayout.setWidthFull();
                horizontalLayout.setAlignItems(FlexComponent.Alignment.CENTER);
                horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
                contentComponent.add(horizontalLayout);

                String dateRangeString = "%s - %s".formatted(
                        DATE_WITHOUT_YEAR.format(range.startDate()),
                        DATE_WITH_YEAR.format(range.endDate()));
                var dateRangeComponent = new Span(dateRangeString);
                dateRangeComponent.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.FontWeight.LIGHT);
                horizontalLayout.add(dateRangeComponent);

                var previousRangeDeltaComponent = new Span(info.previousRangeDelta());
                previousRangeDeltaComponent.setId(PREVIOUS_RANGE_DELTA_COMPONENT_ID);
                horizontalLayout.add(previousRangeDeltaComponent);

                return contentComponent;
            };
        }
    }

    public CrmCard withoutBackground(boolean withoutBackground) {
        this.withoutBackground = withoutBackground;
        getStyle().setBackground(null);
        return this;
    }

    public CrmCard withEllipsisButton(boolean hasEllipsisButton) {
        this.hasEllipsisButton = hasEllipsisButton;
        refreshContent();
        return this;
    }

    public CrmCard withPeriodFilter(boolean hasPeriodFilter) {
        this.hasPeriodFilter = hasPeriodFilter;
        refreshContent();
        return this;
    }

    private void initHeaderAndContent() {
        initHeader();
        updateContentIfNeeded();
    }

    private void setColspan(int colspan) {
        getElement().setAttribute("colspan", String.valueOf(Math.max(1, colspan)));
    }

    private static H4 createDefaultTitleComponent(String title) {
        var titleComponent = new H4(title);
        titleComponent.addClassNames(LumoUtility.Margin.Right.LARGE, LumoUtility.Whitespace.NOWRAP);
        return titleComponent;
    }

    private void initHeader() {
        var messages = applicationContext.getBean(Messages.class);
        var uiComponents = applicationContext.getBean(UiComponents.class);

        var header = uiComponents.create(Div.class);

        var horizontalLayout = new HorizontalLayout();
        horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        horizontalLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        horizontalLayout.add(title);

        if (hasPeriodFilter) {
            var periodFilter = uiComponents.create(DropdownButton.class);
            periodFilter.addThemeVariants(DropdownButtonVariant.LUMO_CONTRAST, DropdownButtonVariant.LUMO_SMALL);
            periodFilter.addClassName(LumoUtility.Margin.Left.AUTO);
            addDropdownItems(messages, periodFilter);
            horizontalLayout.add(periodFilter);
        }

        if (hasEllipsisButton) {
            Button settingsButton = new Button(VaadinIcon.ELLIPSIS_DOTS_V.create());
            settingsButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST,
                    ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
            horizontalLayout.add(settingsButton);
        }

        header.add(horizontalLayout);

        setHeader(header);
    }

    private void addDropdownItems(Messages messages, DropdownButton dropdownButton) {
        CardPeriod[] periods = CardPeriod.values();
        for (CardPeriod period : periods) {
            String localizedMessage = messages.getMessage(period);
            DropdownButtonItem item = dropdownButton.addItem(period.name(), localizedMessage);
            item.addClickListener(e -> {
                if (!Objects.equals(dropdownButton.getText(), localizedMessage)) {
                    currentPeriod = period;
                    updateContentIfNeeded();
                    dropdownButton.setText(localizedMessage);
                }
            });
        }
        dropdownButton.setText(messages.getMessage(CardPeriod.MONTH));
    }

    private void updateContentIfNeeded() {
        if (contentProvider != null) {
            setContent(contentProvider.apply(currentPeriod));
        }
    }

    private void setContent(Component content) {
        removeAll();
        add(content);
        updateBackground();
    }

    private void refreshContent() {
        initHeaderAndContent();
    }

    private void updateBackground() {
        if (withoutBackground) {
            return;
        }

        var deltaComponent = UiComponentUtils.findComponent(this,
                PREVIOUS_RANGE_DELTA_COMPONENT_ID).orElse(this);

        var delta = "";
        if (deltaComponent instanceof HasText hasText) {
            delta = hasText.getText();
        }

        String spanThemeName = "badge";
        if (delta.startsWith("↑")) {
            spanThemeName += " success";
            setLinearGradient("var(--lumo-primary-color-10pct)");
        } else if (delta.startsWith("↓")) {
            spanThemeName += " error";
            setLinearGradient("var(--lumo-error-color-10pct)");
        } else {
            spanThemeName += " default";
            setLinearGradient("var(--lumo-contrast-10pct)");
        }

        if (deltaComponent instanceof Span span) {
            span.getElement().getThemeList().add(spanThemeName);
        }
    }

    private void setLinearGradient(String color) {
        setLinearGradient(this, 45, "var(--lumo-base-color)", color);
    }

    private static void setLinearGradient(HasStyle component, int deg, String color1, String color2) {
        component.getStyle().set("background", "linear-gradient(%ddeg, %s, %s)".formatted(deg, color1, color2));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
