package com.company.crm.ai.view.aiconversation;

import com.company.crm.ai.context.AiContextEntityRegistry;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.service.AiConversationService;
import com.company.crm.app.util.constant.CrmConstants;
import io.jmix.core.AccessManager;
import io.jmix.core.Id;
import io.jmix.core.IdSerialization;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.accesscontext.UiShowViewContext;
import io.jmix.flowui.view.DialogWindow;
import io.jmix.flowui.view.View;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Component
public class AiChatAboutThisSupport {

    private final AiConversationService aiConversationService;
    private final DialogWindows dialogWindows;
    private final IdSerialization idSerialization;
    private final AccessManager accessManager;
    private final AiContextEntityRegistry contextEntityRegistry;

    public AiChatAboutThisSupport(AiConversationService aiConversationService,
                                  DialogWindows dialogWindows,
                                  IdSerialization idSerialization,
                                  AccessManager accessManager,
                                  AiContextEntityRegistry contextEntityRegistry) {
        this.aiConversationService = aiConversationService;
        this.dialogWindows = dialogWindows;
        this.idSerialization = idSerialization;
        this.accessManager = accessManager;
        this.contextEntityRegistry = contextEntityRegistry;
    }

    public void openChatAbout(View<?> origin, Object entity) {
        if (entity == null) {
            return;
        }
        openChatAbout(origin, List.of(entity));
    }

    public void openChatAbout(View<?> origin, Collection<?> entities) {
        if (!isOpenChatPermitted()) {
            return;
        }

        List<String> entityReferences = entityReferences(entities);
        if (entityReferences.isEmpty()) {
            return;
        }

        AiConversation conversation = aiConversationService.createNewConversation();

        DialogWindow<AiConversationDetailView> dialogWindow = dialogWindows.detail(origin, AiConversation.class)
                .editEntity(conversation)
                .withViewClass(AiConversationDetailView.class)
                .withViewConfigurer(view -> view.setInitialEntityReferences(entityReferences))
                .build();

        dialogWindow.setModal(false);
        dialogWindow.setResizable(true);
        dialogWindow.setTop("5%");
        dialogWindow.setLeft("50%");
        dialogWindow.setWidth("48%");
        dialogWindow.setHeight("82%");
        dialogWindow.open();
    }

    public boolean isOpenChatPermitted() {
        UiShowViewContext context = new UiShowViewContext(CrmConstants.ViewIds.AI_CONVERSATION_DETAIL);
        accessManager.applyRegisteredConstraints(context);
        return context.isPermitted();
    }

    private List<String> entityReferences(Collection<?> entities) {
        return new LinkedHashSet<>((entities != null ? entities : List.of()).stream()
                .filter(Objects::nonNull)
                .filter(entity -> contextEntityRegistry.findDefinition(entity.getClass()).isPresent())
                .map(entity -> idSerialization.idToString(Id.of(entity)))
                .toList())
                .stream()
                .toList();
    }
}
