package com.company.crm.view.client;

import com.company.crm.app.feature.queryparameters.tab.TabIndexUrlQueryParameterBinder;
import com.company.crm.model.client.Client;
import com.company.crm.model.client.ClientRepository;
import com.company.crm.model.user.UserActivity;
import com.company.crm.model.user.UserActivityRepository;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.core.FetchPlan;
import io.jmix.core.LoadContext;
import io.jmix.core.SaveContext;
import io.jmix.core.repository.JmixDataRepositoryContext;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
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
public class ClientDetailView extends StandardDetailView<Client> {

    @Autowired
    private ClientRepository repository;
    @Autowired
    private UserActivityRepository userActivityRepository;

    @ViewComponent
    private JmixTabSheet tabSheet;

    @Subscribe
    private void onInit(final InitEvent event) {
        TabIndexUrlQueryParameterBinder.register(this, tabSheet);
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
    private List<UserActivity> activitiesDlLoadFromRepositoryDelegate(final Pageable pageable, final JmixDataRepositoryContext ctx) {
        return userActivityRepository.findAll(pageable, ctx).getContent();
    }
}