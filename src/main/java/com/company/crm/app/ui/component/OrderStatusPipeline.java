package com.company.crm.app.ui.component;

import com.company.crm.model.order.OrderStatus;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.jmix.core.Messages;
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

    public enum DisplayMode {
        AUTO,
        ONE_ROW,
        TWO_COLUMNS,
        ROW_PER_STATUS
    }

    public void setDisplayMode(DisplayMode displayMode) {
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

    public void selectUntil(OrderStatus status) {
        deselectAllStatuses();
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
            var registration = comp.addClickListener(e -> listener.accept(comp));
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
            titleComponent.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.SEMIBOLD, LumoUtility.Background.TRANSPARENT);

            this.status = status;
            this.titleComponent = titleComponent;

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
            getClassNames().add(getSelectedBackgroundClassName());
            removeBorder();
            if (getStatusesWithContrastTitle().contains(status)) {
                titleComponent.addClassName(LumoUtility.TextColor.PRIMARY_CONTRAST);
            }
        }

        public void deselect() {
            getClassNames().remove(getSelectedBackgroundClassName());
            getStyle().setBorder(getSelectedBorderStyle());
            if (getStatusesWithContrastTitle().contains(status)) {
                titleComponent.removeClassName(LumoUtility.TextColor.PRIMARY_CONTRAST);
            }
        }

        private void installDefaultStyles() {
            addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.FULL);
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

        private void removeBorder() {
            getStyle().setBorder(null);
        }

        private String getSelectedBackgroundClassName() {
            return getBackgroundClass(status);
        }

        private String getSelectedBorderStyle() {
            String template = "0.1em solid var(--lumo-%s-color)";
            return switch (status) {
                case NEW -> template.formatted("body-text");
                case ACCEPTED -> template.formatted("primary");
                case IN_PROGRESS -> template.formatted("warning");
                case DONE -> template.formatted("success");
            };
        }

        private List<OrderStatus> getStatusesWithContrastTitle() {
            return List.of(OrderStatus.ACCEPTED, OrderStatus.DONE);
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
