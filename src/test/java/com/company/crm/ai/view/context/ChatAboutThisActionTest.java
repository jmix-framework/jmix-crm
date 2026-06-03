package com.company.crm.ai.view.context;

import com.company.crm.AbstractUiTest;
import com.company.crm.ai.view.conversation.AiConversationStarterView;
import com.company.crm.ai.view.conversation.composer.AiConversationComposerFragment;
import com.company.crm.model.client.Client;
import com.company.crm.model.client.ClientType;
import com.company.crm.view.client.ClientListView;
import io.jmix.core.Id;
import io.jmix.core.IdSerialization;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.event.dialog.DialogOpenedEvent;
import io.jmix.flowui.testassist.UiTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@RecordApplicationEvents
@TestPropertySource(properties = "spring.ai.openai.api-key=test-key")
class ChatAboutThisActionTest extends AbstractUiTest {

    @Autowired
    private IdSerialization idSerialization;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Test
    void opensChatSideDialogForSelectedClientFromRealGrid() {
        Client client = entities.client("Chat Context Client", ClientType.BUSINESS);
        String expectedReference = idSerialization.idToString(Id.of(client));

        viewTestSupport.navigateTo(ClientListView.class);
        ClientListView listView = UiTestUtils.getCurrentView();

        DataGrid<Client> clientsDataGrid = UiTestUtils.getComponent(listView, "clientsDataGrid");
        clientsDataGrid.select(client);

        ChatAboutThisAction<?> chatAboutThis = (ChatAboutThisAction<?>) clientsDataGrid.getAction("chatAboutThis");
        assertThat(chatAboutThis).isNotNull();
        chatAboutThis.execute();

        DialogOpenedEvent openedEvent = applicationEvents.stream(DialogOpenedEvent.class)
                .filter(event -> event.getContentComponents().stream()
                        .anyMatch(AiConversationStarterView.class::isInstance))
                .findFirst()
                .orElseThrow();
        assertThat(openedEvent.getSource().isOpened()).isTrue();

        AiConversationStarterView starterView = openedEvent.getContentComponents().stream()
                .filter(AiConversationStarterView.class::isInstance)
                .map(AiConversationStarterView.class::cast)
                .findFirst()
                .orElseThrow();
        AiConversationComposerFragment composer = viewTestSupport.findDescendant(starterView, AiConversationComposerFragment.class)
                .orElseThrow();

        assertThat(composer.entityReferences()).containsExactly(expectedReference);
    }
}
