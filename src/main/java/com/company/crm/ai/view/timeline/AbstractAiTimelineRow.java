package com.company.crm.ai.view.timeline;

import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.html.Span;

public abstract class AbstractAiTimelineRow extends HorizontalLayout {

    protected final VerticalLayout body;
    protected final HorizontalLayout header;
    protected final Span actor;
    protected final Span time;

    protected AbstractAiTimelineRow() {
        setWidthFull();
        setSpacing(true);
        setPadding(false);
        setAlignItems(FlexComponent.Alignment.START);

        body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);
        body.setWidth("95%");
        body.addClassName("ai-timeline-message-body");

        header = new HorizontalLayout();
        header.setPadding(false);
        header.setSpacing(true);
        header.setAlignItems(FlexComponent.Alignment.BASELINE);
        header.addClassName("ai-timeline-message-header");

        actor = new Span();
        actor.addClassName("ai-timeline-message-actor");
        time = new Span();
        time.addClassName("ai-timeline-message-time");
        header.add(actor, time);
    }

    protected void initRow(boolean assistant, String actorName, String formattedTime) {
        removeAll();
        body.removeAll();
        body.add(header);

        actor.setText(actorName);
        time.setText(formattedTime);

        AiTimelineAvatar avatar = new AiTimelineAvatar(assistant, actorName);
        add(avatar, body);
    }
}
