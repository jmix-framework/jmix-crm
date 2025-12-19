package com.company.crm.view.client;

import com.company.crm.app.feature.queryparameters.tab.TabIndexUrlQueryParameterBinder;
import com.company.crm.app.ui.component.RecentActivitiesBlock;
import com.company.crm.app.util.ui.listener.resize.WidthResizeListener;
import com.company.crm.model.client.Client;
import com.company.crm.model.client.ClientRepository;
import com.company.crm.model.user.activity.UserActivity;
import com.company.crm.model.user.activity.UserActivityRepository;
import com.company.crm.model.user.activity.client.ClientUserActivityRepository;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout.Orientation;
import com.vaadin.flow.router.Route;
import io.jmix.core.FetchPlan;
import io.jmix.core.SaveContext;
import io.jmix.core.repository.JmixDataRepositoryContext;
import io.jmix.flowui.component.splitlayout.JmixSplitLayout;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Route(value = "clients/:id", layout = MainView.class)
@ViewController(id = "Client.detail")
@ViewDescriptor(path = "client-detail-view.xml")
@EditedEntityContainer("clientDc")
@DialogMode(width = "90%", height = "90%", resizable = true, closeOnEsc = true, closeOnOutsideClick = true)
public class ClientDetailView extends StandardDetailView<Client> implements WidthResizeListener {

    @Autowired
    private ClientRepository repository;
    @Autowired
    private ClientUserActivityRepository userActivityRepository;

    @ViewComponent
    private JmixTabSheet tabSheet;
    @ViewComponent
    private RecentActivitiesBlock recentActivities;
    @ViewComponent
    private JmixSplitLayout formSplit;

    @Override
    public void configureUiForWidth(int width) {
        formSplit.setOrientation(width > 900 ? Orientation.HORIZONTAL : Orientation.VERTICAL);
    }

    @Subscribe
    private void onInit(final InitEvent event) {
        TabIndexUrlQueryParameterBinder.register(this, tabSheet);
    }

    @Subscribe
    private void onBeforeShow(final BeforeShowEvent event) {
        recentActivities.setMaxWidth(27.5f, Unit.EM);
        recentActivities.setClient(getEditedEntity());
    }

    @Install(to = "clientDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private Optional<Client> loadDelegate(UUID id, FetchPlan fetchPlan) {
        return repository.findById(id, fetchPlan);
    }

    @Install(target = Target.DATA_CONTEXT)
    private Set<Object> saveDelegate(SaveContext saveContext) {
        return Set.of(repository.save(getEditedEntity()));
    }

    @Install(to = "activitiesDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private List<? extends UserActivity> activitiesDlLoadFromRepositoryDelegate(final Pageable pageable, final JmixDataRepositoryContext ctx) {
        return userActivityRepository.findAll(pageable, ctx).getContent();
    }
}