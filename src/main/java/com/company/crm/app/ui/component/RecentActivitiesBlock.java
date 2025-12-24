package com.company.crm.app.ui.component;

import com.company.crm.app.service.datetime.DateTimeService;
import com.company.crm.app.service.user.UserActivityService;
import com.company.crm.model.client.Client;
import com.company.crm.model.user.User;
import com.company.crm.model.user.activity.UserActivity;
import com.company.crm.view.util.SkeletonStyler;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.jmix.core.Messages;
import io.jmix.flowui.asynctask.UiAsyncTasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class RecentActivitiesBlock extends Div implements ApplicationContextAware, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(RecentActivitiesBlock.class);

    private static final DateTimeFormatter DATE_WITH_YEAR_AND_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private static final String[] BORDER_CLASSNAMES = new String[]{
            LumoUtility.BorderRadius.LARGE,
            LumoUtility.Border.ALL,
            LumoUtility.Padding.MEDIUM
    };

    private final Map<String, LocalDate> loadedActivities = new LinkedHashMap<>();

    private Messages messages;
    private UiAsyncTasks uiAsyncTasks;
    private DateTimeService dateTimeService;
    private UserActivityService userActivityService;

    private User user = null;
    private Client client = null;

    private int activitiesInBlockMaxCount = 3;

    public void setBorder(boolean border) {
        if (border) {
            withBorder();
        } else {
            withoutBorder();
        }
    }

    public RecentActivitiesBlock withBorder() {
        addClassNames(BORDER_CLASSNAMES);
        return this;
    }

    public RecentActivitiesBlock withoutBorder() {
        removeClassNames(BORDER_CLASSNAMES);
        return this;
    }

    public void setClient(Client client) {
        this.client = client;
        reloadContent();
    }

    public void showForUser(User user) {
        this.user = user;
        reloadContent();
    }

    public void setActivitiesInBlockMaxCount(int maxCount) {
        checkArgument(maxCount > 0, "max count should be greater than 0");
        this.activitiesInBlockMaxCount = maxCount;
        reloadContent();
    }

    public void addActivities(String title, LocalDate date) {
        addActivitiesBlock(title, date);
    }

    private void reloadContent() {
        removeAll();
        initComponent();
    }

    private void initComponent() {
        addTitle();
        if (loadedActivities.isEmpty()) {
            loadDefaultActivities();
        } else {
            reloadActivities();
        }
    }

    private void addTitle() {
        H4 title = new H4(messages.getMessage("recentActivitiesTitle"));
        add(title);
    }

    private void reloadActivities() {
        loadedActivities.forEach(this::addActivities);
    }

    private void loadDefaultActivities() {
        addTodayActivities();
        addYesterdayActivities();
    }

    private void addTodayActivities() {
        LocalDate todayStart = dateTimeService.getCurrentDayStart().toLocalDate();
        addActivities("Today", todayStart);
    }

    private void addYesterdayActivities() {
        LocalDate yesterdayStart = dateTimeService.getCurrentDayStart().toLocalDate().minusDays(1);
        addActivities("Yesterday", yesterdayStart);
    }

    private void addActivitiesBlock(String title, LocalDate date) {
        H5 titleComponent = new H5(title);
        titleComponent.addClassNames(LumoUtility.Padding.Bottom.SMALL, LumoUtility.Padding.Top.MEDIUM);
        add(titleComponent);

        Div scrollerContent = new Div();
        scrollerContent.addClassNames(LumoUtility.Padding.Left.MEDIUM);

        Scroller scroller = new Scroller(scrollerContent);
        scroller.setMaxHeight(activitiesInBlockMaxCount * 4, Unit.EM);
        add(scroller);

        loadActivitiesAsync(title, date, scroller);
    }

    private CompletableFuture<Void> loadActivitiesAsync(String title,
                                                        LocalDate date,
                                                        Scroller scroller) {
        SkeletonStyler.apply(scroller);
        return uiAsyncTasks.supplierConfigurer(() -> doLoadActivities(date))
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

    private List<? extends UserActivity> doLoadActivities(LocalDate date) {
        if (user == null && client == null) {
            return userActivityService.loadActivities(date, activitiesInBlockMaxCount);
        } else if (user == null) {
            return userActivityService.loadClientActivities(client, date, activitiesInBlockMaxCount);
        } else if (client == null) {
            return userActivityService.loadActivities(user, date, activitiesInBlockMaxCount);
        } else {
            return userActivityService.loadClientActivities(user, client, date, activitiesInBlockMaxCount);
        }
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

        Span dateSpan = new Span(DATE_WITH_YEAR_AND_TIME.format(requireNonNull(activity.getCreatedDate())));
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
        messages = applicationContext.getBean(Messages.class);
        uiAsyncTasks = applicationContext.getBean(UiAsyncTasks.class);
        dateTimeService = applicationContext.getBean(DateTimeService.class);
        userActivityService = applicationContext.getBean(UserActivityService.class);
    }
}
