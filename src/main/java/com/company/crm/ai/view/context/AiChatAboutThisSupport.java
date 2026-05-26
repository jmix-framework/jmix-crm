package com.company.crm.ai.view.context;

import com.company.crm.ai.view.conversation.AiConversationStarterView;
import com.company.crm.ai.view.conversation.component.AiSideDialogHeader;
import com.company.crm.ai.context.AiContextEntityRegistry;
import com.company.crm.app.util.constant.CrmConstants;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.jmix.core.AccessManager;
import io.jmix.core.Id;
import io.jmix.core.IdSerialization;
import io.jmix.core.Messages;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.Views;
import io.jmix.flowui.component.sidedialog.SideDialog;
import io.jmix.flowui.accesscontext.UiShowViewContext;
import io.jmix.flowui.kit.component.sidedialog.SideDialogPosition;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class AiChatAboutThisSupport {

    private final IdSerialization idSerialization;
    private final AccessManager accessManager;
    private final AiContextEntityRegistry contextEntityRegistry;
    private final Dialogs dialogs;
    private final Views views;
    private final Messages messages;
    private final UiComponents uiComponents;

    public AiChatAboutThisSupport(IdSerialization idSerialization,
                                  AccessManager accessManager,
                                  AiContextEntityRegistry contextEntityRegistry,
                                  Dialogs dialogs,
                                  Views views,
                                  Messages messages,
                                  UiComponents uiComponents) {
        this.idSerialization = idSerialization;
        this.accessManager = accessManager;
        this.contextEntityRegistry = contextEntityRegistry;
        this.dialogs = dialogs;
        this.views = views;
        this.messages = messages;
        this.uiComponents = uiComponents;
    }

    public void openChatAbout(Object entity) {
        if (entity == null) {
            return;
        }
        openChatAbout(List.of(entity));
    }

    public void openChatAbout(Collection<?> entities) {
        if (!isOpenChatPermitted()) {
            return;
        }

        List<String> entityReferences = entityReferences(entities);
        if (entityReferences.isEmpty()) {
            return;
        }

        AiConversationStarterView starterView = views.create(AiConversationStarterView.class);
        starterView.setOpenedInDialog(true);
        starterView.setInitialEntityReferences(entityReferences);

        SideDialog sideDialog = dialogs.createSideDialog()
                .withSideDialogPosition(SideDialogPosition.RIGHT)
                .withHorizontalSize("35%")
                .withModal(false)
                .withContentComponents(starterView)
                .withHeaderProvider(sd -> createChatSideDialogHeader(sd, messages.getMessage("com.company.crm.ai.view.conversation", "aiConversationStarterView.title")))
                .build();

        starterView.setParentSideDialog(sideDialog);
        sideDialog.open();
        starterView.activateStarterView();
    }

    private HorizontalLayout createChatSideDialogHeader(SideDialog sideDialog, String title) {
        AiSideDialogHeader header = uiComponents.create(AiSideDialogHeader.class);
        header.setDialog(sideDialog, title);
        return header;
    }

    public boolean isOpenChatPermitted() {
        UiShowViewContext context = new UiShowViewContext(CrmConstants.ViewIds.AI_CONVERSATION_DETAIL);
        accessManager.applyRegisteredConstraints(context);
        return context.isPermitted();
    }

    private List<String> entityReferences(Collection<?> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
                .filter(Objects::nonNull)
                .filter(entity -> contextEntityRegistry.findDefinition(entity.getClass()).isPresent())
                .map(entity -> idSerialization.idToString(Id.of(entity)))
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                ));
    }
}
