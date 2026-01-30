package com.company.crm.app.online;

import com.company.crm.app.annotation.OnlineProfile;
import com.company.crm.app.ui.component.CrmLoader;
import com.company.crm.app.util.init.DemoDataGenerator;
import com.vaadin.flow.component.dialog.Dialog;
import io.jmix.core.Messages;
import io.jmix.core.session.SessionData;
import io.jmix.flowui.backgroundtask.BackgroundTask;
import io.jmix.flowui.backgroundtask.BackgroundTaskHandler;
import io.jmix.flowui.backgroundtask.BackgroundWorker;
import io.jmix.flowui.backgroundtask.TaskLifeCycle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static com.company.crm.app.util.ui.CrmUiUtils.reloadCurrentPage;

/**
 * In online demo mode, generates demo data in a background task.
 */
@Component
@OnlineProfile
@Scope(value = WebApplicationContext.SCOPE_SESSION)
public class OnlineDemoDataCreator {

    private static final String DEMO_DATA_CREATED_FLAG = "demo-data-created";

    private final Messages messages;
    private final SessionData sessionData;
    private final DemoDataGenerator generator;
    private final BackgroundWorker backgroundWorker;

    private Dialog demoDataDialog;
    private CrmLoader demoDataLoader;

    public OnlineDemoDataCreator(Messages messages, SessionData sessionData,
                                 DemoDataGenerator generator, BackgroundWorker backgroundWorker) {
        this.messages = messages;
        this.sessionData = sessionData;
        this.generator = generator;
        this.backgroundWorker = backgroundWorker;
    }

    public void createDemoDataIfNeeded() {
        Object demoDataCreated = sessionData.getAttribute(DEMO_DATA_CREATED_FLAG);
        if (!Boolean.TRUE.equals(demoDataCreated)) {
            createDemoData();
        }
    }

    private void createDemoData() {
        openDemoDataLoader();
        startDemoDataGeneratorInBackground();
        sessionData.setAttribute(DEMO_DATA_CREATED_FLAG, true);
    }

    private void openDemoDataLoader() {
        if (demoDataDialog == null) {
            demoDataLoader = new CrmLoader();
            demoDataLoader.addClassName("demo-data-loader");
            demoDataLoader.startLoading();

            demoDataDialog = new Dialog(demoDataLoader);
            demoDataDialog.setCloseOnEsc(false);
            demoDataDialog.setCloseOnOutsideClick(false);
            demoDataDialog.setModal(true);
            demoDataDialog.addClassName("demo-data-loader-dialog");
        }

        demoDataLoader.setLoadingMessage(messages.getMessage("demoData.progress.configuring"));

        if (!demoDataDialog.isOpened()) {
            demoDataDialog.open();
        }
    }

    private void startDemoDataGeneratorInBackground() {
        BackgroundTaskHandler<Void> handler = backgroundWorker.handle(new GenerateDemoDataTask());
        handler.execute();
    }

    private class GenerateDemoDataTask extends BackgroundTask<String, Void> {

        protected GenerateDemoDataTask() {
            super(60);
        }

        @Override
        public Void run(TaskLifeCycle<String> taskLifeCycle) {
            generator.initDemoDataIfNeeded(message -> publishProgress(taskLifeCycle, message));
            return null;
        }

        @Override
        public void progress(List<String> changes) {
            if (changes.isEmpty() || demoDataLoader == null) {
                return;
            }
            demoDataLoader.setLoadingMessage(changes.getLast());
        }

        @Override
        public void done(Void result) {
            if (demoDataLoader != null) {
                demoDataLoader.setLoadingMessage("Demo data is ready \uD83D\uDC4D");
            }
            closeDemoDataLoader();
            reloadCurrentPage();
        }
    }

    private void publishProgress(TaskLifeCycle<String> taskLifeCycle, String message) {
        if (taskLifeCycle.isInterrupted()) {
            return;
        }
        try {
            taskLifeCycle.publish(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void closeDemoDataLoader() {
        if (demoDataDialog != null && demoDataDialog.isOpened()) {
            demoDataDialog.close();
        }
    }
}
