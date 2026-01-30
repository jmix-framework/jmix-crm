package com.company.crm.app.online;

import com.company.crm.app.annotation.OnlineProfile;
import com.company.crm.app.util.init.DemoDataGenerator;
import com.vaadin.flow.component.notification.Notification;
import io.jmix.core.session.SessionData;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.backgroundtask.BackgroundTask;
import io.jmix.flowui.backgroundtask.BackgroundTaskHandler;
import io.jmix.flowui.backgroundtask.BackgroundWorker;
import io.jmix.flowui.backgroundtask.TaskLifeCycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

/**
 * In online demo mode, generates demo data in a background task.
 */
@Component
@OnlineProfile
@Scope(value = WebApplicationContext.SCOPE_SESSION)
public class OnlineDemoDataCreator {

    @Autowired
    private SessionData sessionData;
    @Autowired
    private Notifications notifications;
    @Autowired
    private DemoDataGenerator generator;
    @Autowired
    private BackgroundWorker backgroundWorker;

    public void createDemoData() {
        Object demoDataCreated = sessionData.getAttribute("demo-data-created");
        if (!Boolean.TRUE.equals(demoDataCreated)) {
            notifications.create("Generating demo data...")
                    .withPosition(Notification.Position.BOTTOM_END)
                    .show();

            BackgroundTaskHandler<Void> handler = backgroundWorker.handle(new GenerateDemoDataTask());
            handler.execute();

            sessionData.setAttribute("demo-data-created", true);
        }
    }

    private class GenerateDemoDataTask extends BackgroundTask<Integer, Void> {

        protected GenerateDemoDataTask() {
            super(60);
        }

        @Override
        public Void run(TaskLifeCycle<Integer> taskLifeCycle) {
            generator.initDemoDataIfNeeded();
            return null;
        }

        @Override
        public void done(Void result) {
            notifications.create("Demo data are created 👍")
                    .withType(Notifications.Type.SUCCESS)
                    .withPosition(Notification.Position.BOTTOM_END)
                    .show();
        }
    }
}