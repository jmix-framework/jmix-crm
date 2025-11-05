package com.company.crm.view.home;

import com.company.crm.app.util.ui.component.model.CrmComponentModel;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.jmix.core.Messages;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.card.JmixCard;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButton;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButtonItem;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButtonVariant;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.text.CaseUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public record CardModel(String id,
                        int colspan,
                        Component title,
                        Function<CardPeriod, CardContentModel> contentModelProvider
) implements CrmComponentModel<JmixCard> {

    public CardModel(String id,
                     int colspan,
                     String title,
                     Function<CardPeriod, CardContentModel> contentProvider
    ) {
        this(id, colspan, createDefaultTitleComponent(title), contentProvider);
    }

    private static H4 createDefaultTitleComponent(String title) {
        var titleComponent = new H4(title);
        titleComponent.addClassNames(LumoUtility.Margin.Right.LARGE, LumoUtility.Whitespace.NOWRAP);
        return titleComponent;
    }

    public interface CardContentModel {
        default ComposedCardContentModel composeWith(CardContentModel other) {
            return new ComposedCardContentModel(this, other);
        }

        default boolean hasPeriodFilter() {
            return true;
        }

        default boolean hasEllipsisButton() {
            return true;
        }

        default void creationCallback(JmixCard card) {
        }
    }

    @SuppressWarnings("unchecked")
    public static abstract class AbstractCardContentModel<SELF extends AbstractCardContentModel<?>> implements CardContentModel {

        private boolean hasPeriodFilter = true;
        private boolean hasEllipsisButton = true;
        private Consumer<JmixCard> creationCallback = card -> {
        };

        @Override
        public boolean hasPeriodFilter() {
            return hasPeriodFilter;
        }

        @Override
        public boolean hasEllipsisButton() {
            return hasEllipsisButton;
        }

        @Override
        public void creationCallback(JmixCard card) {
            creationCallback.accept(card);
        }

        public SELF withHasPeriodFilter(boolean hasPeriodFilter) {
            this.hasPeriodFilter = hasPeriodFilter;
            return (SELF) this;
        }

        public SELF withHasEllipsisButton(boolean hasEllipsisButton) {
            this.hasEllipsisButton = hasEllipsisButton;
            return (SELF) this;
        }

        public SELF withCreationCallback(Consumer<JmixCard> creationCallback) {
            this.creationCallback = creationCallback;
            return (SELF) this;
        }
    }

    public static final class DefaultCardContentModel extends AbstractCardContentModel<DefaultCardContentModel> {

        private final String content;
        private final String dateRange;
        private final String statInfo;

        public DefaultCardContentModel(String content,
                                       String dateRange,
                                       String statInfo) {
            this.content = content;
            this.dateRange = dateRange;
            this.statInfo = statInfo;
        }

        public String content() {
            return content;
        }

        public String dateRange() {
            return dateRange;
        }

        public String statInfo() {
            return statInfo;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (DefaultCardContentModel) obj;
            return new EqualsBuilder()
                    .append(this.content, that.content)
                    .append(this.dateRange, that.dateRange)
                    .append(this.statInfo, that.statInfo)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return Objects.hash(content, dateRange, statInfo);
        }
    }

    public static final class ComponentCardContentModel extends AbstractCardContentModel<ComponentCardContentModel> {

        private final Component content;

        public ComponentCardContentModel(Component content) {
            this.content = content;
        }

        public Component content() {
            return content;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (ComponentCardContentModel) obj;
            return new EqualsBuilder()
                    .append(this.content, that.content)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return Objects.hash(content);
        }

    }

    public static final class ComposedCardContentModel extends AbstractCardContentModel<ComposedCardContentModel> {

        private final CardContentModel first;
        private final CardContentModel second;

        public ComposedCardContentModel(CardContentModel first,
                                        CardContentModel second) {
            this.first = first;
            this.second = second;
        }

        public CardContentModel first() {
            return first;
        }

        public CardContentModel second() {
            return second;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (ComposedCardContentModel) obj;
            return new EqualsBuilder()
                    .append(this.first, that.first)
                    .append(this.second, that.second)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }
    }

    @Override
    public Class<JmixCard> getComponentClass() {
        return JmixCard.class;
    }

    @Override
    public void fillComponent(JmixCard card, ApplicationContext applicationContext) {
        card.setId(id);
        card.setMinWidth(20, Unit.EM);
        card.addThemeVariants(CardVariant.LUMO_ELEVATED, CardVariant.LUMO_OUTLINED);
        card.getElement().setAttribute("colspan", String.valueOf(Math.max(1, colspan)));

        CardPeriod initialPeriod = CardPeriod.MONTH;
        CardContentModel contentModel = contentModelProvider.apply(initialPeriod);

        addHeader(card, contentModel, applicationContext);

        Collection<Component> content = createContent(contentModel, initialPeriod);
        doSetContent(card, content);

        contentModel.creationCallback(card);
    }

    private void addHeader(JmixCard card, CardContentModel contentModel, ApplicationContext applicationContext) {
        var messages = applicationContext.getBean(Messages.class);
        var uiComponents = applicationContext.getBean(UiComponents.class);

        var header = uiComponents.create(Div.class);

        var horizontalLayout = new HorizontalLayout();
        horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        horizontalLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        header.add(horizontalLayout);

        horizontalLayout.add(title);

        if (contentModel.hasPeriodFilter()) {
            var periodFilter = uiComponents.create(DropdownButton.class);
            periodFilter.addThemeVariants(DropdownButtonVariant.LUMO_CONTRAST, DropdownButtonVariant.LUMO_SMALL);
            periodFilter.addClassName(LumoUtility.Margin.Left.AUTO);
            addDropdownItems(card, messages, periodFilter);
            horizontalLayout.add(periodFilter);
        }

        if (contentModel.hasEllipsisButton()) {
            Button settingsButton = new Button(VaadinIcon.ELLIPSIS_DOTS_V.create());
            settingsButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST,
                    ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
            horizontalLayout.add(settingsButton);
        }

        card.setHeader(header);
    }

    private void addDropdownItems(JmixCard card, Messages messages, DropdownButton dropdownButton) {
        CardPeriod[] periods = CardPeriod.values();
        for (CardPeriod period : periods) {
            String localizedMessage = messages.getMessage(period);
            DropdownButtonItem item = dropdownButton.addItem(period.name(), localizedMessage);
            item.addClickListener(e -> {
                if (!dropdownButton.getText().equals(localizedMessage)) {
                    createAndSetContent(card, period);
                    dropdownButton.setText(localizedMessage);
                }
            });
        }
        dropdownButton.setText(messages.getMessage(CardPeriod.MONTH));
    }

    private void createAndSetContent(JmixCard card, CardPeriod period) {
        CardContentModel contentModel = contentModelProvider.apply(period);
        Collection<Component> content = createContent(contentModel, period);
        doSetContent(card, content);
        contentModel.creationCallback(card);
    }

    private void doSetContent(JmixCard card, Collection<Component> content) {
        card.removeAll();
        card.add(content);
    }

    private Collection<Component> createContent(CardContentModel model, CardPeriod period) {
        return switch (model) {
            case DefaultCardContentModel defaultCardContentModel -> List.of(
                    createDefaultContentProvider(p ->
                            defaultCardContentModel).apply(period)
            );

            case ComponentCardContentModel componentCardContentModel -> List.of(
                    componentCardContentModel.content()
            );

            case ComposedCardContentModel composedCardContentModel -> {
                Collection<Component> first = createContent(composedCardContentModel.first(), period);
                Collection<Component> second = createContent(composedCardContentModel.second(), period);
                Collection<Component> result = new ArrayList<>(first);
                result.addAll(second);
                yield result;
            }

            default -> throw new IllegalStateException("Unexpected value: " + model);
        };
    }

    private static Function<CardPeriod, Component> createDefaultContentProvider(
            Function<CardPeriod, DefaultCardContentModel> cardContentProvider) {
        return period -> {
            var cardContent = cardContentProvider.apply(period);
            var contentComponent = new Div(new H1(cardContent.content()));

            var horizontalLayout = new HorizontalLayout();
            horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            horizontalLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            contentComponent.add(horizontalLayout);

            var periodComponent = new Span(cardContent.dateRange());
            periodComponent.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.FontWeight.LIGHT);
            horizontalLayout.add(periodComponent);

            String statInfo = cardContent.statInfo();
            var statInfoComponent = new Span(statInfo);
            var themeName = statInfo.startsWith("↓")
                    ? "error"
                    : statInfo.startsWith("↑")
                    ? "success" : "contrast";
            statInfoComponent.getElement().getThemeList().add("badge " + themeName);
            horizontalLayout.add(statInfoComponent);

            return contentComponent;
        };
    }
}
