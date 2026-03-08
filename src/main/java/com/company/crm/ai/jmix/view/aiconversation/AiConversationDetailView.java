package com.company.crm.ai.jmix.view.aiconversation;

import com.company.crm.ai.entity.AiConversation;
import com.company.crm.ai.entity.AiAttachmentType;
import com.company.crm.ai.entity.AiConversationAttachment;
import com.company.crm.ai.entity.ChatMessage;
import com.company.crm.ai.entity.ChatMessageType;
import com.company.crm.app.ui.component.GridEmptyStateComponent;
import com.company.crm.app.service.ai.CrmAnalyticsService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.upload.FileRejectedEvent;
import com.vaadin.flow.component.upload.SucceededEvent;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.FileRef;
import io.jmix.core.FileStorage;
import io.jmix.core.FileStorageLocator;
import io.jmix.core.Messages;
import io.jmix.core.MetadataTools;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.app.inputdialog.DialogActions;
import io.jmix.flowui.app.inputdialog.DialogOutcome;
import io.jmix.flowui.app.inputdialog.InputParameter;
import io.jmix.flowui.asynctask.UiAsyncTasks;
import io.jmix.flowui.component.upload.JmixUpload;
import io.jmix.flowui.component.upload.receiver.FileTemporaryStorageBuffer;
import io.jmix.flowui.component.upload.receiver.TemporaryStorageFileData;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.upload.TemporaryStorage;
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
    private static final String CRM_FILE_STORAGE_NAME = "crm";

    @ViewComponent
    private InstanceContainer<AiConversation> aiConversationDc;
    @ViewComponent
    private CollectionContainer<AiConversationAttachment> attachmentsDc;
    @ViewComponent
    private CollectionLoader<AiConversationAttachment> attachmentsDl;
    @ViewComponent
    private VerticalLayout chatPanel;
    @ViewComponent
    private VerticalLayout attachmentsPanel;
    @ViewComponent
    private Component attachmentsGridLayout;
    @ViewComponent
    private JmixUpload attachmentUpload;
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
    @Autowired
    private Dialogs dialogs;
    @Autowired
    private Messages messages;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private Notifications notifications;
    @Autowired
    private TemporaryStorage temporaryStorage;
    @Autowired
    private FileStorageLocator fileStorageLocator;

    private MessageList messageList;
    private MessageInput messageInput;
    private ProgressBar progressBar;
    private GridEmptyStateComponent attachmentsEmptyState;

    @Subscribe
    public void onInit(final InitEvent event) {
        initDynamicComponentsIfNeeded();
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {

        setShowSaveNotification(false);

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

    @Subscribe("attachmentUpload")
    public void onAttachmentUploadSucceeded(final SucceededEvent event) {
        AiConversation conversation = aiConversationDc.getItemOrNull();
        TemporaryStorageFileData uploadedFileData = resolveUploadedFileData(event);

        if (conversation == null) {
            removeTemporaryUpload(uploadedFileData);
            attachmentUpload.clearFileList();
            notifications.create(messageBundle.getMessage("attachmentUploadNoConversation"))
                    .withType(Notifications.Type.ERROR)
                    .show();
            return;
        }

        String uploadedFileName = resolveUploadedFileName(
                event.getFileName(),
                uploadedFileData != null ? uploadedFileData.getFileName() : null
        );
        FileRef uploadedFileRef = moveUploadedFileToStorage(uploadedFileData, uploadedFileName);
        if (uploadedFileRef == null) {
            attachmentUpload.clearFileList();
            notifications.create(messageBundle.getMessage("attachmentUploadMissingFile"))
                    .withType(Notifications.Type.ERROR)
                    .show();
            return;
        }

        try {
            markConversationStartedIfNeeded(conversation);

            AiConversationAttachment attachment = dataManager.create(AiConversationAttachment.class);
            attachment.setConversation(conversation);
            attachment.setFile(uploadedFileRef);
            attachment.setFileName(uploadedFileName);
            attachment.setTitle(uploadedFileName);
            attachment.setType(AiAttachmentType.USER_UPLOADED);
            attachment = dataManager.save(attachment);

            attachmentUpload.clearFileList();
            loadAttachments();

            String actorName = resolveCurrentActorName();
            String uploadEventText = buildUploadEventText(actorName, uploadedFileName);
            messageList.addItem(userUploadMessageListItem(uploadEventText, now()));
            submitUploadToAi(conversation, attachment, event.getMIMEType(), uploadedFileName, actorName);
        } catch (Exception e) {
            removeTemporaryUpload(uploadedFileData);
            attachmentUpload.clearFileList();
            log.error("Failed to persist uploaded attachment", e);
            notifications.create(messageBundle.getMessage("attachmentUploadPersistError"))
                    .withType(Notifications.Type.ERROR)
                    .show();
        }
    }

    @Subscribe("attachmentUpload")
    public void onAttachmentUploadFileRejected(final FileRejectedEvent event) {
        notifications.create(event.getErrorMessage())
                .withType(Notifications.Type.WARNING)
                .show();
    }

    @Subscribe("editConversationTitleBtn")
    public void onEditConversationTitleBtnClick(final ClickEvent<JmixButton> event) {
        AiConversation conversation = aiConversationDc.getItemOrNull();
        if (conversation == null || conversation.getId() == null) {
            return;
        }

        String currentTitle = conversation.getTitle() == null ? "" : conversation.getTitle();

        dialogs.createInputDialog(this)
                .withHeader(messageBundle.getMessage("editConversationTitleDialog.header"))
                .withLabelsPosition(Dialogs.InputDialogBuilder.LabelsPosition.TOP)
                .withParameters(
                        InputParameter.stringParameter("title")
                                .withLabel(messageBundle.getMessage("editConversationTitleDialog.titleField"))
                                .withRequired(true)
                                .withDefaultValue(currentTitle)
                )
                .withActions(DialogActions.OK_CANCEL)
                .withCloseListener(closeEvent -> {
                    if (!closeEvent.closedWith(DialogOutcome.OK)) {
                        return;
                    }

                    String updatedTitle = closeEvent.getValue("title");
                    if (updatedTitle == null || updatedTitle.isBlank()) {
                        return;
                    }

                    String sanitizedTitle = updatedTitle.trim();
                    AiConversation editableConversation = aiConversationDc.getItemOrNull();
                    if (editableConversation == null) {
                        return;
                    }

                    editableConversation.setTitle(sanitizedTitle);
                    getViewData().getDataContext().save();

                    reloadViewData();
                })
                .open();
    }

    private void onMessageSubmit(MessageInput.SubmitEvent event) {
        String userMessage = event.getValue();
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return;
        }

        AiConversation conversation = aiConversationDc.getItemOrNull();
        if (conversation == null || conversation.getId() == null) {
            log.warn("Cannot submit message: AiConversation item is null or has no ID");
            return;
        }

        try {
            markConversationStartedIfNeeded(conversation);
        } catch (Exception e) {
            log.error("Failed to mark conversation as started before message submit", e);
            notifications.create("Failed to start conversation. Please try again.")
                    .withType(Notifications.Type.ERROR)
                    .show();
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
                    reloadViewData();
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

    private void submitUploadToAi(
            AiConversation conversation,
            AiConversationAttachment attachment,
            String mimeType,
            String uploadedFileName,
            String actorName
    ) {
        progressBar.setVisible(true);
        messageInput.setEnabled(false);

        uiAsyncTasks.supplierConfigurer(() ->
                        crmAnalyticsService.processAttachmentUpload(
                                conversation.getId().toString(),
                                attachment.getId(),
                                uploadedFileName,
                                mimeType,
                                actorName
                        )
                )
                .withResultHandler(response -> {
                    messageList.addItem(assistantMessageListItem(response, now()));
                    reloadViewData();
                    focusInput();
                })
                .withExceptionHandler(e -> {
                    String errorId = UUID.randomUUID().toString().substring(0, 8);
                    log.error("Error processing attachment upload async [Error ID: {}]", errorId, e);
                    String errorMessage = "I'm sorry, I couldn't analyze the uploaded file right now. "
                            + "Please try again later or contact support with Error ID: " + errorId;
                    messageList.addItem(assistantMessageListItem(errorMessage, now()));
                    focusInput();
                })
                .supplyAsync();
    }

    private void loadAttachments() {
        AiConversation conversation = aiConversationDc.getItemOrNull();
        if (conversation == null || conversation.getId() == null) {
            attachmentsDl.setParameter("conversationId", null);
            attachmentsDc.setItems(List.of());
            updateAttachmentsEmptyState();
            return;
        }
        attachmentsDl.setParameter("conversationId", conversation.getId());
        attachmentsDl.load();
    }

    private void updateAttachmentsEmptyState() {
        if (attachmentsEmptyState == null) {
            return;
        }
        boolean empty = attachmentsDc.getItems().isEmpty();
        attachmentsEmptyState.setVisible(empty);
        attachmentsGridLayout.setVisible(!empty);
    }

    private void refreshMessages() {
        AiConversation conversation = aiConversationDc.getItemOrNull();
        if (messageList == null || conversation == null) {
            return;
        }

        List<ChatMessage> chatMessages = Optional.ofNullable(conversation.getMessages()).orElse(List.of());
        List<MessageListItem> messageListItems = chatMessages.stream()
                .map(this::createMessageListItem)
                .toList();
        messageList.setItems(messageListItems);
    }

    private void reloadViewData() {
        prepareAttachmentsLoaderParameter();
        getViewData().loadAll();
        loadAttachments();
        refreshMessages();
    }

    private void prepareAttachmentsLoaderParameter() {
        AiConversation conversation = aiConversationDc.getItemOrNull();
        UUID conversationId = conversation != null ? conversation.getId() : null;
        attachmentsDl.setParameter("conversationId", conversationId);
    }

    private void markConversationStartedIfNeeded(AiConversation conversation) {
        if (Boolean.TRUE.equals(conversation.getFirstMessageSent())) {
            return;
        }
        conversation.setFirstMessageSent(true);
        getViewData().getDataContext().save();
    }

    private MessageListItem createMessageListItem(ChatMessage message) {
        ChatMessageType messageType = message.getType();
        if (ChatMessageType.ASSISTANT.equals(messageType)) {
            return assistantMessageListItem(message.getContent(), message.getCreatedDate());
        }
        if (ChatMessageType.USER_UPLOAD.equals(messageType) || ChatMessageType.ATTACHMENT.equals(messageType)) {
            return userUploadMessageListItem(message.getContent(), message.getCreatedDate());
        }
        return userMessageListItem(message.getContent(), message.getCreatedDate());
    }

    private MessageListItem assistantMessageListItem(String content, OffsetDateTime createdAt) {
        MessageListItem item = new MessageListItem(content, createdAt.toInstant(), messageBundle.getMessage("assistantName"));
        item.setUserColorIndex(2);
        return item;
    }

    private MessageListItem userMessageListItem(String content, OffsetDateTime createdAt) {
        UserDetails user = currentAuthentication.getUser();
        String userName = resolveCurrentActorName();
        MessageListItem item = new MessageListItem(content, createdAt.toInstant(), userName);
        item.setUserAbbreviation(user.getUsername().substring(0, 1));
        item.setUserColorIndex(1);
        return item;
    }

    private MessageListItem userUploadMessageListItem(String content, OffsetDateTime createdAt) {
        MessageListItem item = new MessageListItem(content, createdAt.toInstant(), messageBundle.getMessage("uploadEventName"));
        item.setUserAbbreviation("AT");
        item.addClassNames("attachment-event");
        return item;
    }

    private OffsetDateTime now() {
        return timeSource.now().toOffsetDateTime();
    }

    private String resolveUploadedFileName(String fileNameFromEvent, String fileNameFromBuffer) {
        if (fileNameFromEvent != null && !fileNameFromEvent.isBlank()) {
            return fileNameFromEvent;
        }
        if (fileNameFromBuffer != null && !fileNameFromBuffer.isBlank()) {
            return fileNameFromBuffer;
        }
        return "uploaded-file";
    }

    private FileRef moveUploadedFileToStorage(TemporaryStorageFileData uploadedFileData, String uploadedFileName) {
        if (uploadedFileData == null
                || uploadedFileData.getFileInfo() == null
                || uploadedFileData.getFileInfo().getId() == null) {
            return null;
        }
        FileStorage fileStorage = fileStorageLocator.getByName(CRM_FILE_STORAGE_NAME);
        return temporaryStorage.putFileIntoStorage(
                uploadedFileData.getFileInfo().getId(),
                uploadedFileName,
                fileStorage
        );
    }

    private TemporaryStorageFileData resolveUploadedFileData(SucceededEvent event) {
        if (event.getUpload().getReceiver() instanceof FileTemporaryStorageBuffer storageBuffer) {
            return storageBuffer.getFileData();
        }
        return null;
    }

    private void removeTemporaryUpload(TemporaryStorageFileData uploadedFileData) {
        if (uploadedFileData == null
                || uploadedFileData.getFileInfo() == null
                || uploadedFileData.getFileInfo().getId() == null) {
            return;
        }
        try {
            temporaryStorage.deleteFile(uploadedFileData.getFileInfo().getId());
        } catch (Exception e) {
            log.warn("Failed to cleanup temporary upload {}", uploadedFileData.getFileInfo().getId(), e);
        }
    }

    private String buildUploadEventText(String actorName, String uploadedFileName) {
        return messageBundle.formatMessage("attachmentUploadEventMessage", actorName, uploadedFileName);
    }

    private String resolveCurrentActorName() {
        UserDetails user = currentAuthentication.getUser();
        String userName = metadataTools.getInstanceName(user);
        if (userName != null && !userName.isBlank()) {
            return userName;
        }
        return user != null && user.getUsername() != null && !user.getUsername().isBlank()
                ? user.getUsername()
                : "User";
    }

    private void focusInput() {
        progressBar.setVisible(false);
        messageInput.setEnabled(true);
        messageInput.focus();
    }

    private void initDynamicComponentsIfNeeded() {
        if (messageList == null) {
            messageList = uiComponents.create(MessageList.class);
            messageList.setSizeFull();
            messageList.setMarkdown(true);
            messageList.addClassName("ai-conversation-message-list");
        }
        if (messageInput == null) {
            messageInput = uiComponents.create(MessageInput.class);
            messageInput.setWidthFull();
            messageInput.addSubmitListener(this::onMessageSubmit);
        }
        if (progressBar == null) {
            progressBar = uiComponents.create(ProgressBar.class);
            progressBar.setWidthFull();
            progressBar.setIndeterminate(true);
            progressBar.setVisible(false);
        }
        if (attachmentsEmptyState == null) {
            attachmentsEmptyState = new GridEmptyStateComponent(messages.getMessage("defaultGridEmptyStateText"));
            attachmentsEmptyState.setSizeFull();
            attachmentsEmptyState.setVisible(false);
        }

        if (chatPanel != null && messageList.getParent().isEmpty()) {
            chatPanel.add(messageList, progressBar, messageInput);
            chatPanel.setFlexGrow(1, messageList);
        }
        if (attachmentsPanel != null && attachmentsEmptyState.getParent().isEmpty()) {
            attachmentsPanel.addComponentAtIndex(2, attachmentsEmptyState);
            attachmentsPanel.setFlexGrow(1, attachmentsEmptyState);
        }
    }

    int getRenderedAttachmentCount() {
        return attachmentsDc.getItems().size();
    }

    boolean hasRenderedAttachment(UUID attachmentId) {
        return attachmentsDc.getItems().stream()
                .anyMatch(attachment -> attachmentId.equals(attachment.getId()));
    }
}
