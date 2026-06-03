package com.company.crm.ai.view.timeline;

import com.company.crm.app.icons.CrmIcons;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Div;

public class AiTimelineAvatar extends Div {

    public AiTimelineAvatar(boolean assistant, String actorName) {
        if (assistant) {
            addClassNames("ai-timeline-avatar", "ai-timeline-avatar-assistant");
            add(CrmIcons.SPARKLES.create());
        } else {
            Avatar userAvatar = new Avatar(actorName);
            userAvatar.addClassName("ai-timeline-avatar");
            add(userAvatar);
        }
    }
}
