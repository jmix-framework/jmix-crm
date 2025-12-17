package com.company.crm.app.ui.component;

import com.company.crm.app.service.datetime.DateTimeService;
import com.company.crm.app.service.user.UserService;
import com.company.crm.model.user.User;
import com.company.crm.model.user.UserActivity;
import com.company.crm.view.util.SkeletonStyler;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.jmix.flowui.asynctask.UiAsyncTasks;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RecentActivitiesBlock extends Div implements ApplicationContextAware, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(RecentActivitiesBlock.class);

    private static final DateTimeFormatter DATE_WITH_YEAR_AND_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final Map<String, LocalDate> loadedActivities = new LinkedHashMap<>();
    private int activitiesMaxCount = 3;

    private UserService userService;
    private UiAsyncTasks uiAsyncTasks;
    private DateTimeService dateTimeService;

    public void addActivities(String title, LocalDate date) {
        addSeparator();
        addActivitiesBlock(title, date);
    }

    public void setActivitiesMaxCount(int activitiesMaxCount) {
        this.activitiesMaxCount = activitiesMaxCount;
        removeAll();
        initComponent();
    }

    private void initComponent() {
        if (loadedActivities.isEmpty()) {
            loadDefaultActivities();
        } else {
            reloadActivities();
        }
    }

    private void reloadActivities() {
        loadedActivities.forEach(this::addActivities);
    }

    private void loadDefaultActivities() {
        addTodayActivities();
        addYesterdayActivities();
    }

    private void addTodayActivities() {
        LocalDate todayStart = dateTimeService.getDayStart().toLocalDate();
        addActivities("Today", todayStart);
    }

    private void addYesterdayActivities() {
        LocalDate yesterdayStart = dateTimeService.getDayStart().toLocalDate().minusDays(1);
        addActivities("Yesterday", yesterdayStart);
    }

    private void addSeparator() {
        if (getComponentCount() == 0) {
            return;
        }
        HorizontalLayout separator = new HorizontalLayout();
        separator.setWidthFull();
        separator.setHeight(1, Unit.EM);
        add(separator);
    }

    private void addActivitiesBlock(String title, LocalDate date) {
        H5 titleComponent = new H5(title);
        titleComponent.addClassNames(LumoUtility.Padding.Bottom.SMALL);
        add(titleComponent);

        Div scrollerContent = new Div();
        scrollerContent.addClassNames(LumoUtility.Padding.Left.MEDIUM);

        Scroller scroller = new Scroller(scrollerContent);
        scroller.setWidthFull();
        scroller.setMaxHeight(10, Unit.EM);
        add(scroller);

        loadActivitiesAsync(title, date, scroller);
    }

    private CompletableFuture<Void> loadActivitiesAsync(String title,
                                                        LocalDate date,
                                                        Scroller scroller) {
        SkeletonStyler.apply(scroller);
        return uiAsyncTasks.supplierConfigurer(() -> userService.loadActivities(date, activitiesMaxCount))
                .withResultHandler(activities -> {
                    SkeletonStyler.remove(scroller);
                    HasComponents scrollerContent = (HasComponents) scroller.getContent();
                    if (activities.isEmpty()) {
                        scrollerContent.add(createEmptyRow(title));
                    } else {
                        for (UserActivity activity : activities) {
                            Component row = createActivityRow(activity);
                            scrollerContent.add(row);
                        }
                    }
                    loadedActivities.put(title, date);
                })
                .withExceptionHandler(e -> {
                    SkeletonStyler.remove(scroller);
                    log.warn("Error when loading activities", e);
                }).supplyAsync();
    }

    private static Component createEmptyRow(String title) {
        Span span = new Span("No activities found for %s...".formatted(title.toLowerCase()));
        span.addClassNames(LumoUtility.Padding.Top.MEDIUM);
        return span;
    }

    private Component createActivityRow(UserActivity activity) {
        User user = activity.getUser();
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);

        Avatar avatar = new Avatar(user.getUsername().substring(0, 1));
        row.add(avatar);

        Span userNameSpan = new Span(user.getFullName());
        userNameSpan.addClassNames(LumoUtility.TextColor.BODY);

        Span activityDescriptionSpan = new Span(activity.getActionDescription());
        activityDescriptionSpan.addClassNames(LumoUtility.TextColor.TERTIARY);

        Span dateSpan = new Span(DATE_WITH_YEAR_AND_TIME.format(activity.getCreatedDate()));
        dateSpan.addClassNames(LumoUtility.TextColor.TERTIARY);

        Div activityInfoBlock = new Div(new HorizontalLayout(userNameSpan, activityDescriptionSpan), dateSpan);
        activityInfoBlock.addClassNames(LumoUtility.Padding.Bottom.SMALL);
        row.add(activityInfoBlock);

        return row;
    }

    @Override
    public void afterPropertiesSet() {
        initComponent();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        autowireDependencies(applicationContext);
    }

    private void autowireDependencies(ApplicationContext applicationContext) {
        userService = applicationContext.getBean(UserService.class);
        uiAsyncTasks = applicationContext.getBean(UiAsyncTasks.class);
        dateTimeService = applicationContext.getBean(DateTimeService.class);
    }
}
