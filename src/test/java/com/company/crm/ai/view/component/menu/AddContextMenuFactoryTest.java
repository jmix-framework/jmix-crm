package com.company.crm.ai.view.component.menu;

import com.company.crm.AbstractUiTest;
import com.company.crm.ai.context.AiContextEntityRegistry;
import com.company.crm.model.client.Client;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.menubar.MenuBar;
import io.jmix.core.Messages;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.upload.JmixUpload;
import io.jmix.flowui.view.MessageBundle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AddContextMenuFactoryTest extends AbstractUiTest {

    @Autowired
    private UiComponents uiComponents;

    @Autowired
    private AiContextEntityRegistry contextEntityRegistry;

    @Autowired
    private ObjectProvider<MessageBundle> messageBundles;

    @Autowired
    private Messages messages;

    @Test
    void uploadMenuItemUsesPluralLocalizedLabel() {
        JmixUpload upload = uiComponents.create(JmixUpload.class);
        AddContextMenuFactory factory = new AddContextMenuFactory((label, entityClass) -> {
        }, contextEntityRegistry, conversationMessageBundle());

        factory.createAddMenuBar(upload);

        assertThat(viewTestSupport.textsByClassName(upload.getUploadButton(), "ai-timeline-add-menu-item-label"))
                .containsExactly(messages.getMessage("com.company.crm.ai.view.conversation/uploadFileAction"));
    }

    @Test
    void crmEntitySubMenuListsConfiguredEntitiesAndInvokesLookupCallback() {
        AtomicReference<Class<?>> selectedClass = new AtomicReference<>();
        AddContextMenuFactory factory = new AddContextMenuFactory(
                (label, entityClass) -> selectedClass.set(entityClass),
                contextEntityRegistry,
                conversationMessageBundle()
        );

        MenuBar menuBar = factory.createAddMenuBar(uiComponents.create(JmixUpload.class));
        MenuItem addItem = menuBar.getItems().getFirst();
        MenuItem crmEntityItem = addItem.getSubMenu().getItems().stream()
                .filter(item -> viewTestSupport.textsByClassName(item, "ai-timeline-add-menu-item-label")
                        .contains(messages.getMessage("com.company.crm.ai.view.conversation/addCrmEntityAction")))
                .findFirst()
                .orElseThrow();

        List<String> entityLabels = crmEntityItem.getSubMenu().getItems().stream()
                .flatMap(item -> viewTestSupport.textsByClassName(item, "ai-timeline-add-menu-item-label").stream())
                .toList();
        assertThat(entityLabels).contains("Tasks", "Categories", "Clients");

        MenuItem clientsItem = crmEntityItem.getSubMenu().getItems().stream()
                .filter(item -> viewTestSupport.textsByClassName(item, "ai-timeline-add-menu-item-label").contains("Clients"))
                .findFirst()
                .orElseThrow();
        ComponentUtil.fireEvent(clientsItem, new ClickEvent<>(clientsItem, false, 0, 0, 0, 0,
                0, 0, false, false, false, false));

        assertThat(selectedClass).hasValue(Client.class);
    }

    private MessageBundle conversationMessageBundle() {
        MessageBundle messageBundle = messageBundles.getObject();
        messageBundle.setMessageGroup("com.company.crm.ai.view.conversation");
        return messageBundle;
    }
}
