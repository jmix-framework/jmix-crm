package com.company.crm.ai.view.timeline;

import com.company.crm.AbstractUiTest;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.icon.Icon;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiTimelineAvatarTest extends AbstractUiTest {

    @Test
    void assistantAvatarUsesSparklesGlyphAndAssistantClass() {
        AiTimelineAvatar avatar = new AiTimelineAvatar(true, "CRM AI");

        assertThat(avatar.getClassNames())
                .contains("ai-timeline-avatar", "ai-timeline-avatar-assistant");
        Icon icon = viewTestSupport.findDescendant(avatar, Icon.class).orElseThrow();
        assertThat(icon.getElement().getAttribute("icon")).isEqualTo("crm:sparkles");
    }

    @Test
    void userAvatarUsesStandardAvatarStyles() {
        AiTimelineAvatar avatar = new AiTimelineAvatar(false, "Mary David");

        assertThat(avatar.getClassNames()).doesNotContain("ai-timeline-avatar-assistant");
        Avatar vaadinAvatar = viewTestSupport.findDescendant(avatar, Avatar.class).orElseThrow();
        assertThat(vaadinAvatar.getName()).isEqualTo("Mary David");
        assertThat(vaadinAvatar.getClassNames()).contains("ai-timeline-avatar");
    }
}
