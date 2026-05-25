package com.company.crm.ai.view.aiconversation;

import com.company.crm.ai.context.AiContextEntityDefinition;
import com.company.crm.ai.context.AiContextEntityRegistry;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.jmix.flowui.component.upload.JmixUpload;
import io.jmix.flowui.view.MessageBundle;

import java.util.function.BiConsumer;

class AddContextMenuFactory {

    private final BiConsumer<String, Class<?>> openEntityLookup;
    private final AiContextEntityRegistry contextEntityRegistry;
    private final MessageBundle messageBundle;

    AddContextMenuFactory(BiConsumer<String, Class<?>> openEntityLookup,
                          AiContextEntityRegistry contextEntityRegistry,
                          MessageBundle messageBundle) {
        this.openEntityLookup = openEntityLookup;
        this.contextEntityRegistry = contextEntityRegistry;
        this.messageBundle = messageBundle;
    }

    MenuBar createAddMenuBar(JmixUpload attachmentUpload) {
        configureAttachmentUploadForMenu(attachmentUpload);

        // TODO: eigene klasse, siehe AiAttachmentCard
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_ICON);
        menuBar.addClassName("ai-timeline-add-menu");
        menuBar.setOverlayClassName("ai-timeline-add-menu-overlay");

        Icon addIcon = VaadinIcon.PLUS.create();
        addIcon.setSize("var(--lumo-icon-size-s)");

        MenuItem addItem = menuBar.addItem(addIcon);
        addItem.setAriaLabel(messageBundle.getMessage("addContextAction"));

        SubMenu subMenu = addItem.getSubMenu();
        subMenu.addItem(attachmentUpload, null);

        MenuItem crmEntityItem = subMenu.addItem(
                createMenuItemContent(VaadinIcon.DATABASE, messageBundle.getMessage("addCrmEntityAction")),
                null
        );
        SubMenu crmEntitySubMenu = crmEntityItem.getSubMenu();
        contextEntityRegistry.addMenuDefinitions()
                .forEach(definition -> addEntityLookupMenuItem(crmEntitySubMenu, definition));

        return menuBar;
    }

    private void addEntityLookupMenuItem(SubMenu subMenu, AiContextEntityDefinition definition) {
        String label = messageBundle.getMessage(definition.menuMessageKey());
        subMenu.addItem(createMenuItemContent(definition.icon(), label),
                event -> openEntityLookup.accept(label, definition.entityClass()));
    }

    private void configureAttachmentUploadForMenu(JmixUpload attachmentUpload) {
        attachmentUpload.setDropAllowed(false);
        attachmentUpload.setUploadButton(createMenuItemContent(VaadinIcon.UPLOAD, messageBundle.getMessage("uploadFileAction")));
        attachmentUpload.addClassName("ai-timeline-menu-upload");
    }

    private Component createMenuItemContent(VaadinIcon iconName, String labelText) {
        // TODO: eigene klasse, siehe AiAttachmentCard
        HorizontalLayout content = new HorizontalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.addClassName("ai-timeline-add-menu-item");

        Icon icon = iconName.create();
        icon.addClassName("ai-timeline-add-menu-item-icon");

        Span label = new Span(labelText);
        label.addClassName("ai-timeline-add-menu-item-label");

        content.add(icon, label);
        return content;
    }
}
