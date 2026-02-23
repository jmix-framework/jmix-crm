package com.company.crm.ai.jmix.view.aiconversation;

import com.company.crm.ai.entity.AiConversation;
import com.company.crm.ai.entity.AiConversationAttachment;
import com.company.crm.ai.entity.ChatMessage;
import com.company.crm.ai.entity.ChatMessageType;
import com.company.crm.app.service.ai.CrmAnalyticsService;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.Route;
import io.jmix.core.MetadataTools;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.asynctask.UiAsyncTasks;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.MessageBundle;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Route(value = "ai-conversations/:id", layout = MainView.class)
@ViewController(id = "AiConversation.detail")
@ViewDescriptor(path = "ai-conversation-detail-view.xml")
@EditedEntityContainer("aiConversationDc")
public class AiConversationDetailView extends StandardDetailView<AiConversation> {

    private static final Logger log = LoggerFactory.getLogger(AiConversationDetailView.class);

    @ViewComponent
    private InstanceContainer<AiConversation> aiConversationDc;
    @ViewComponent
    private CollectionContainer<AiConversationAttachment> attachmentsDc;
    @ViewComponent
    private CollectionLoader<AiConversationAttachment> attachmentsDl;
    @ViewComponent
    private VerticalLayout chatPanel;
    @ViewComponent
    private Span attachmentsEmptyState;
    @ViewComponent
    private MessageBundle messageBundle;

    @Autowired
    private CrmAnalyticsService crmAnalyticsService;
    @Autowired
    private CurrentAuthentication currentAuthentication;
    @Autowired
    private UiAsyncTasks uiAsyncTasks;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private TimeSource timeSource;
    @Autowired
    private MetadataTools metadataTools;

    private MessageList messageList;
    private MessageInput messageInput;
    private ProgressBar progressBar;

    @Subscribe
    public void onReady(final ReadyEvent event) {
        messageList = uiComponents.create(MessageList.class);
        messageList.setSizeFull();
        messageList.setMarkdown(true);

        messageInput = uiComponents.create(MessageInput.class);
        messageInput.setWidthFull();
        messageInput.addSubmitListener(this::onMessageSubmit);

        progressBar = uiComponents.create(ProgressBar.class);
        progressBar.setWidthFull();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        chatPanel.add(messageList, progressBar, messageInput);
        chatPanel.setFlexGrow(1, messageList);

        loadAttachments();
        refreshMessages();
        focusInput();
    }

    @Subscribe(id = "aiConversationDl", target = Target.DATA_LOADER)
    public void onAiConversationDlPostLoad(final io.jmix.flowui.model.InstanceLoader.PostLoadEvent<AiConversation> event) {
        loadAttachments();
        refreshMessages();
    }

    @Subscribe(id = "attachmentsDl", target = Target.DATA_LOADER)
    public void onAttachmentsDlPostLoad(final CollectionLoader.PostLoadEvent<AiConversationAttachment> event) {
        updateAttachmentsEmptyState();
    }

    private void onMessageSubmit(MessageInput.SubmitEvent event) {
        String userMessage = event.getValue();
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return;
        }

        AiConversation conversation = aiConversationDc.getItemOrNull();
        if (conversation == null) {
            log.warn("Cannot submit message: AiConversation item is null");
            return;
        }

        messageList.addItem(userMessageListItem(userMessage, now()));

        progressBar.setVisible(true);
        messageInput.setEnabled(false);

        uiAsyncTasks.supplierConfigurer(() ->
                        crmAnalyticsService.processBusinessQuestion(userMessage, conversation.getId().toString())
                )
                .withResultHandler(response -> {
                    messageList.addItem(assistantMessageListItem(response, now()));
                    getViewData().loadAll();
                    loadAttachments();
                    refreshMessages();
                    focusInput();
                })
                .withExceptionHandler(e -> {
                    String errorId = UUID.randomUUID().toString().substring(0, 8);
                    log.error("Error processing message async [Error ID: {}]", errorId, e);
                    String errorMessage = "I'm sorry, I encountered a technical error while processing your request. "
                            + "Please try again later or contact support with Error ID: " + errorId;
                    messageList.addItem(assistantMessageListItem(errorMessage, now()));
                    focusInput();
                })
                .supplyAsync();
    }

    private void loadAttachments() {
        AiConversation conversation = aiConversationDc.getItemOrNull();
        if (conversation == null) {
            attachmentsDc.setItems(List.of());
            updateAttachmentsEmptyState();
            return;
        }
        attachmentsDl.setParameter("conversation", conversation);
        attachmentsDl.load();
    }

    private void updateAttachmentsEmptyState() {
        attachmentsEmptyState.setVisible(attachmentsDc.getItems().isEmpty());
    }

    private void refreshMessages() {
        if (messageList == null || aiConversationDc.getItemOrNull() == null) {
            return;
        }

        List<ChatMessage> messages = Optional.ofNullable(aiConversationDc.getItem().getMessages()).orElse(List.of());
        List<MessageListItem> messageListItems = messages.stream()
                .map(this::createMessageListItem)
                .toList();
        messageList.setItems(messageListItems);
    }

    private MessageListItem createMessageListItem(ChatMessage message) {
        boolean isAssistant = ChatMessageType.ASSISTANT.equals(message.getType());
        return isAssistant
                ? assistantMessageListItem(message.getContent(), message.getCreatedDate())
                : userMessageListItem(message.getContent(), message.getCreatedDate());
    }

    private MessageListItem assistantMessageListItem(String content, OffsetDateTime createdAt) {
        MessageListItem item = new MessageListItem(content, createdAt.toInstant(), messageBundle.getMessage("assistantName"));
        item.setUserColorIndex(2);
        return item;
    }

    private MessageListItem userMessageListItem(String content, OffsetDateTime createdAt) {
        UserDetails user = currentAuthentication.getUser();
        String userName = metadataTools.getInstanceName(user);
        MessageListItem item = new MessageListItem(content, createdAt.toInstant(), userName);
        item.setUserAbbreviation(user.getUsername().substring(0, 1));
        item.setUserColorIndex(1);
        return item;
    }

    private OffsetDateTime now() {
        return timeSource.now().toOffsetDateTime();
    }

    private void focusInput() {
        progressBar.setVisible(false);
        messageInput.setEnabled(true);
        messageInput.focus();
    }

    int getRenderedAttachmentCount() {
        return attachmentsDc.getItems().size();
    }

    boolean hasRenderedAttachment(UUID attachmentId) {
        return attachmentsDc.getItems().stream()
                .anyMatch(attachment -> attachmentId.equals(attachment.getId()));
    }
}
