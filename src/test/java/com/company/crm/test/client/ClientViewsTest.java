package com.company.crm.test.client;

import com.company.crm.AbstractUiTest;
import com.company.crm.ai.view.conversation.AiConversationStarterView;
import com.company.crm.ai.view.conversation.composer.AiConversationComposerFragment;
import com.company.crm.model.client.Client;
import com.company.crm.view.client.ClientDetailView;
import com.company.crm.view.client.ClientListView;
import io.jmix.core.Id;
import io.jmix.core.IdSerialization;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.testassist.UiTestUtils;
import io.jmix.flowui.testassist.dialog.DialogInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "spring.ai.openai.api-key=test-key")
class ClientViewsTest extends AbstractUiTest {

    @Autowired
    private IdSerialization idSerialization;

    @Test
    void opensClientListView() {
        var view = viewTestSupport.navigateTo(ClientListView.class);
        assertThat(view).isInstanceOf(ClientListView.class);
    }

    @Test
    void opensClientDetailView() {
        var view = viewTestSupport.navigateToNewEntityDetail(Client.class, ClientDetailView.class);
        assertThat(view).isInstanceOf(ClientDetailView.class);
    }

    @Test
    void chatAboutThisActionIsEnabledOnlyWhenClientIsSelected() {
        Client client = entities.client("Client Action State");
        ClientListView view = viewTestSupport.navigateTo(ClientListView.class);
        DataGrid<Client> clientsDataGrid = UiTestUtils.getComponent(view, "clientsDataGrid");
        JmixButton chatButton = UiTestUtils.getComponent(view, "chatAboutThisButton");
        Action chatAction = clientsDataGrid.getAction("chatAboutThis");

        chatAction.refreshState();
        assertThat(chatAction.isEnabled()).isFalse();
        assertThat(chatButton.isEnabled()).isFalse();

        clientsDataGrid.select(client);
        chatAction.refreshState();

        assertThat(chatAction.isEnabled()).isTrue();
        assertThat(chatButton.isEnabled()).isTrue();
    }

    @Test
    void chatAboutThisActionOpensSideDialogWithSelectedClientContext() {
        Client client = entities.client("Client Side Dialog");
        String expectedReference = idSerialization.idToString(Id.of(client));
        ClientListView view = viewTestSupport.navigateTo(ClientListView.class);
        DataGrid<Client> clientsDataGrid = UiTestUtils.getComponent(view, "clientsDataGrid");

        clientsDataGrid.select(client);
        clientsDataGrid.getAction("chatAboutThis").actionPerform(clientsDataGrid);

        AiConversationComposerFragment composer = openedComposer();
        assertThat(composer.entityReferences()).containsExactly(expectedReference);
    }

    @Test
    void chatAboutThisActionPropagatesMultipleSelectedClientsToComposerGrid() {
        Client first = entities.client("Client Multi One");
        Client second = entities.client("Client Multi Two");
        Set<String> expectedReferences = Set.of(
                idSerialization.idToString(Id.of(first)),
                idSerialization.idToString(Id.of(second))
        );
        ClientListView view = viewTestSupport.navigateTo(ClientListView.class);
        DataGrid<Client> clientsDataGrid = UiTestUtils.getComponent(view, "clientsDataGrid");

        clientsDataGrid.select(Set.of(first, second));
        clientsDataGrid.getAction("chatAboutThis").actionPerform(clientsDataGrid);

        AiConversationComposerFragment composer = openedComposer();
        assertThat(composer.entityReferences()).containsExactlyInAnyOrderElementsOf(expectedReferences);
        assertThat(viewTestSupport.textsByClassName(composer, "ai-timeline-attachment-title"))
                .contains("Client Multi One", "Client Multi Two");
    }

    private AiConversationComposerFragment openedComposer() {
        DialogInfo dialogInfo = UiTestUtils.getOpenedDialogs().stream()
                .filter(dialog -> dialog.getContentComponents().stream()
                        .anyMatch(AiConversationStarterView.class::isInstance))
                .reduce((first, second) -> second)
                .orElseThrow();
        assertThat(dialogInfo.getDialog().isOpened()).isTrue();

        AiConversationStarterView starterView = dialogInfo.getContentComponents().stream()
                .filter(AiConversationStarterView.class::isInstance)
                .map(AiConversationStarterView.class::cast)
                .findFirst()
                .orElseThrow();
        return viewTestSupport.findDescendant(starterView, AiConversationComposerFragment.class).orElseThrow();
    }
}
