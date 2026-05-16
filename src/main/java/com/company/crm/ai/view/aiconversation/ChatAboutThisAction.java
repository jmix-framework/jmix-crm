package com.company.crm.ai.view.aiconversation;

import com.vaadin.flow.component.Component;
import io.jmix.flowui.action.ActionType;
import io.jmix.flowui.action.list.ItemTrackingAction;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.view.View;
import org.springframework.beans.factory.annotation.Autowired;

@ActionType(ChatAboutThisAction.ID)
public class ChatAboutThisAction<E> extends ItemTrackingAction<E> {

    public static final String ID = "ai_chatAboutThis";

    private AiChatAboutThisSupport aiChatAboutThisSupport;

    public ChatAboutThisAction() {
        this(ID);
    }

    public ChatAboutThisAction(String id) {
        super(id);
    }

    @Autowired
    public void setAiChatAboutThisSupport(AiChatAboutThisSupport aiChatAboutThisSupport) {
        this.aiChatAboutThisSupport = aiChatAboutThisSupport;
    }

    @Override
    public void execute() {
        checkTarget();
        if (!(target instanceof Component targetComponent)) {
            throw new IllegalStateException("%s target is not a component".formatted(getClass().getSimpleName()));
        }

        View<?> origin = UiComponentUtils.getView(targetComponent);
        aiChatAboutThisSupport.openChatAbout(origin, target.getSelectedItems());
    }

    @Override
    protected boolean isApplicable() {
        return super.isApplicable()
                || target != null && !target.getSelectedItems().isEmpty();
    }

    @Override
    protected boolean isPermitted() {
        return aiChatAboutThisSupport != null && aiChatAboutThisSupport.isOpenChatPermitted();
    }
}
