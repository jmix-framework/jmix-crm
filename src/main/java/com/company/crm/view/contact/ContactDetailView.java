package com.company.crm.view.contact;

import com.company.crm.model.client.Client;
import com.company.crm.model.contact.Contact;
import com.company.crm.model.contact.ContactRepository;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.core.FetchPlan;
import io.jmix.core.SaveContext;
import io.jmix.flowui.component.combobox.EntityComboBox;
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

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Route(value = "contacts/:id", layout = MainView.class)
@ViewController(id = "Contact.detail")
@ViewDescriptor(path = "contact-detail-view.xml")
@EditedEntityContainer("contactDc")
@DialogMode(width = "64em", resizable = true)
public class ContactDetailView extends StandardDetailView<Contact> {

    @Autowired
    private ContactRepository repository;
    @ViewComponent
    private EntityComboBox<Client> clientField;

    @Subscribe
    private void onInitEntity(final InitEntityEvent<Contact> event) {
        clientField.setReadOnly(event.getEntity().getClient() != null);
    }

    @Install(to = "contactDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private Optional<Contact> loadDelegate(UUID id, FetchPlan fetchPlan) {
        return repository.findById(id, fetchPlan);
    }

    @Install(target = Target.DATA_CONTEXT)
    private Set<Object> saveDelegate(SaveContext saveContext) {
        return Set.of(repository.save(getEditedEntity()));
    }
}