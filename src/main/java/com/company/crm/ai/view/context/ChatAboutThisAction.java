package com.company.crm.ai.view.context;

import io.jmix.flowui.action.ActionType;
import io.jmix.flowui.action.list.ItemTrackingAction;
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
        aiChatAboutThisSupport.openChatAbout(target.getSelectedItems());
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
