package com.company.crm.app.ui.component;

import com.company.crm.model.order.OrderStatus;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.Background;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import com.vaadin.flow.theme.lumo.LumoUtility.TextColor;
import com.vaadin.flow.theme.lumo.LumoUtility.TextOverflow;
import com.vaadin.flow.theme.lumo.LumoUtility.Whitespace;
import io.jmix.core.Messages;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.company.crm.app.util.ui.CrmUiUtils.setCursorPointer;
import static com.company.crm.app.util.ui.color.EnumClassColors.getBackgroundClass;
import static java.lang.Math.divideExact;

public class OrderStatusPipeline extends FormLayout implements ApplicationContextAware, InitializingBean {

    private Messages messages;

    private DisplayMode displayMode = DisplayMode.AUTO;

    public enum DisplayMode {
        AUTO,
        ONE_ROW,
        TWO_COLUMNS,
        ROW_PER_STATUS
    }

    public void setDisplayMode(DisplayMode displayMode) {
        this.displayMode = displayMode;
        int statusCount = OrderStatus.values().length;
        switch (displayMode) {
            case ONE_ROW -> setResponsiveSteps(new ResponsiveStep("0", statusCount));
            case TWO_COLUMNS -> setResponsiveSteps(new ResponsiveStep("0", 2));
            case ROW_PER_STATUS -> setResponsiveSteps(new ResponsiveStep("0", 1));
            case AUTO -> setResponsiveSteps(
                    new ResponsiveStep("0", 1),
                    new ResponsiveStep("35em", divideExact(statusCount, 2)),
                    new ResponsiveStep("40em", statusCount)
            );
        }
    }

    public void selectAllStatuses() {
        selectStatus(OrderStatus.values());
    }

    public void deselectAllStatuses() {
        deselectStatus(OrderStatus.values());
    }

    public void selectUntil(@Nullable OrderStatus status) {
        deselectAllStatuses();

        if (status == null) {
            return;
        }

        for (OrderStatus orderStatus : OrderStatus.values()) {
            selectStatus(orderStatus);
            if (orderStatus.equals(status)) {
                break;
            }
        }
    }

    public void selectStatus(OrderStatus... statuses) {
        getStatusComponents().forEach(component -> {
            for (OrderStatus status : statuses) {
                if (component.getStatus().equals(status)) {
                    component.select();
                }
            }
        });
    }

    public void deselectStatus(OrderStatus... statuses) {
        getStatusComponents().forEach(component -> {
            for (OrderStatus status : statuses) {
                if (component.getStatus().equals(status)) {
                    component.deselect();
                }
            }
        });
    }

    public Map<OrderStatus, Registration> addStatusClickListener(Consumer<OrderStatusComponent> listener) {
        var registrations = new HashMap<OrderStatus, Registration>();
        getStatusComponents().forEach(comp -> {
            var registration = comp.addClickListener(e -> {
                if (e.isFromClient()) {
                    listener.accept(comp);
                }
            });
            registrations.put(comp.getStatus(), registration);
        });
        return registrations;
    }

    public Stream<OrderStatusComponent> getStatusComponents() {
        return getChildren()
                .filter(OrderStatusComponent.class::isInstance)
                .map(OrderStatusComponent.class::cast);
    }

    private void initComponent() {
        for (OrderStatus status : OrderStatus.values()) {
            add(new OrderStatusComponent(status));
        }
        setDisplayMode(DisplayMode.AUTO);
        deselectAllStatuses();
    }

    public class OrderStatusComponent extends HorizontalLayout {

        private final OrderStatus status;
        private final Span titleComponent;

        public OrderStatusComponent(OrderStatus status) {
            Span titleComponent = new Span(messages.getMessage(status));
            titleComponent.addClassNames(
                    FontSize.LARGE, FontWeight.SEMIBOLD,
                    TextOverflow.ELLIPSIS, Whitespace.NOWRAP,
                    Background.TRANSPARENT);

            this.status = status;
            this.titleComponent = titleComponent;

            addClassNames(LumoUtility.Margin.Bottom.XSMALL);
            add(titleComponent);
            installSize();
            installItemPositioning();
            installDefaultStyles();
            deselect();
        }

        public OrderStatus getStatus() {
            return status;
        }

        public void setTitle(String title) {
            titleComponent.setText(title);
        }

        public String getTitle() {
            return titleComponent.getText();
        }

        public void select() {
            configureActualStyles(true);
        }

        public void deselect() {
            configureActualStyles(false);
        }

        private void configureActualStyles(boolean selected) {
            setActualTheme(selected);
            setActualBackground(selected);
            setActualTextColor(selected);
        }

        private void setActualTheme(boolean selected) {
            ThemeList themeList = getThemeList();
            themeList.remove(selected ? "deselected" : "selected");
            themeList.add(selected ? "selected" : "deselected");
        }

        private void setActualBackground(boolean selected) {
            String bg = getBackgroundClass(status);
            if (selected) {
                getClassNames().add(bg);
            } else {
                getClassNames().remove(bg);
            }
        }

        private void setActualTextColor(boolean selected) {
            if (List.of(OrderStatus.ACCEPTED, OrderStatus.DONE).contains(status)) {
                if (selected) {
                    titleComponent.addClassName(TextColor.PRIMARY_CONTRAST);
                } else {
                    titleComponent.removeClassName(TextColor.PRIMARY_CONTRAST);
                }
            }
        }

        private void installDefaultStyles() {
            addClassNames("order-status-arrow", "order-status-" + status.getId());
            setCursorPointer(this);
        }

        private void installItemPositioning() {
            setAlignItems(Alignment.CENTER);
            setJustifyContentMode(JustifyContentMode.CENTER);
        }

        private void installSize() {
            setWidth(10, Unit.EM);
            setHeight(2, Unit.EM);
        }
    }

    @Override
    public void afterPropertiesSet() {
        initComponent();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        autowireBeans(applicationContext);
    }

    private void autowireBeans(ApplicationContext applicationContext) {
        this.messages = applicationContext.getBean(Messages.class);
    }
}
