package com.company.crm.ai.view.conversation.composer;

import com.company.crm.ai.view.component.card.AiConversationContextCardFactory;
import com.company.crm.ai.view.component.menu.AddContextMenuFactory;
import com.company.crm.ai.context.AiContextEntityRegistry;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.service.PendingAttachmentInput;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.FailedEvent;
import com.vaadin.flow.component.upload.FileRejectedEvent;
import com.vaadin.flow.component.upload.SucceededEvent;
import io.jmix.core.IdSerialization;
import io.jmix.core.Metadata;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.upload.JmixUpload;
import io.jmix.core.FileRef;
import io.jmix.core.FileStorage;
import io.jmix.core.FileStorageLocator;
import io.jmix.flowui.upload.TemporaryStorage;
import io.jmix.flowui.component.upload.receiver.MultiFileTemporaryStorageBuffer;
import io.jmix.flowui.component.upload.receiver.FileTemporaryStorageBuffer;
import io.jmix.flowui.component.upload.receiver.TemporaryStorageFileData;
import io.jmix.flowui.DialogWindows;
import io.jmix.core.Id;
import com.company.crm.app.service.storage.CrmFileStorage;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.fragment.FragmentUtils;
import io.jmix.flowui.view.MessageBundle;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import com.company.crm.app.util.common.StreamUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

@FragmentDescriptor("ai-conversation-composer-fragment.xml")
public class AiConversationComposerFragment extends Fragment<VerticalLayout> {

    private static final Logger log = LoggerFactory.getLogger(AiConversationComposerFragment.class);
    private static final Consumer<Submission> NOOP_SUBMIT_HANDLER = submission -> {
    };
    private static final Consumer<AiConversationAttachment> NOOP_ATTACHMENT_DOWNLOAD_HANDLER = attachment -> {
    };
    private static final Consumer<String> NOOP_ENTITY_REFERENCE_OPEN_HANDLER = entityReference -> {
    };

    @ViewComponent
    private HorizontalLayout inputBar;
    @ViewComponent
    private VerticalLayout pendingContextLayout;
    @ViewComponent
    private JmixUpload attachmentUpload;
    @ViewComponent
    private MessageBundle messageBundle;
    @Autowired
    private IdSerialization idSerialization;
    @Autowired
    private Metadata metadata;
    @Autowired
    private Notifications notifications;
    @Autowired
    private AiContextEntityRegistry contextEntityRegistry;
    @Autowired
    private DialogWindows dialogWindows;
    @Autowired
    private TemporaryStorage temporaryStorage;
    @Autowired
    private FileStorageLocator fileStorageLocator;

    private final List<String> entityReferences = new ArrayList<>();
    private final List<PendingAttachmentInput> attachments = new ArrayList<>();
    private MessageInput messageInput;
    private MenuBar addMenuBar;
    private AiConversationContextCardFactory contextCardFactory;
    private AddContextMenuFactory addContextMenuFactory;
    private Consumer<Submission> submitHandler = NOOP_SUBMIT_HANDLER;
    private Consumer<AiConversationAttachment> pendingAttachmentDownloadHandler = NOOP_ATTACHMENT_DOWNLOAD_HANDLER;
    private Consumer<String> pendingEntityReferenceOpenHandler = NOOP_ENTITY_REFERENCE_OPEN_HANDLER;
    private Variant variant = Variant.TIMELINE;
    private boolean inputEnabled = true;

    public AiConversationComposerFragment() {
        addReadyListener(this::onReady);
    }

    public void setVariant(Variant variant) {
        this.variant = Objects.requireNonNull(variant);
        applyVariantClassNames();
        applyVariantSpecificLayout();
    }

    public void setSubmitHandler(Consumer<Submission> submitHandler) {
        this.submitHandler = Objects.requireNonNullElse(submitHandler, NOOP_SUBMIT_HANDLER);
    }

    public void setPendingContextActions(Consumer<AiConversationAttachment> pendingAttachmentDownloadHandler,
                                         Consumer<String> pendingEntityReferenceOpenHandler) {
        this.pendingAttachmentDownloadHandler = Objects.requireNonNullElse(
                pendingAttachmentDownloadHandler,
                NOOP_ATTACHMENT_DOWNLOAD_HANDLER
        );
        this.pendingEntityReferenceOpenHandler = Objects.requireNonNullElse(
                pendingEntityReferenceOpenHandler,
                NOOP_ENTITY_REFERENCE_OPEN_HANDLER
        );
        contextCardFactory = null;
        refreshPendingContextLayout();
    }

    public void setInputEnabled(boolean enabled) {
        inputEnabled = enabled;
        if (messageInput != null) {
            messageInput.setEnabled(enabled);
        }
    }

    public void setEnabledAndFocus(boolean enabled) {
        setInputEnabled(enabled);
        if (enabled) {
            focus();
        }
    }

    public void focus() {
        if (messageInput == null) {
            return;
        }

        messageInput.focus();
    }

    public void clear() {
        entityReferences.clear();
        attachments.clear();
        refreshPendingContextLayout();
    }

    public void addEntityReferences(Collection<String> refs) {
        StreamUtils.safeStream(refs)
                .filter(ref -> !entityReferences.contains(ref))
                .forEach(entityReferences::add);
        refreshPendingContextLayout();
    }

    public void addAttachments(Collection<PendingAttachmentInput> inputs) {
        StreamUtils.safeStream(inputs)
                .filter(input -> !attachments.contains(input))
                .forEach(attachments::add);
        refreshPendingContextLayout();
    }

    public void removeEntityReference(String entityReference) {
        entityReferences.remove(entityReference);
        refreshPendingContextLayout();
    }

    public void removeAttachment(PendingAttachmentInput attachment) {
        attachments.remove(attachment);
        refreshPendingContextLayout();
    }

    public List<String> entityReferences() {
        return List.copyOf(entityReferences);
    }

    public List<PendingAttachmentInput> attachments() {
        return List.copyOf(attachments);
    }

    public boolean isEmpty() {
        return entityReferences.isEmpty() && attachments.isEmpty();
    }

    public void configureTimelineLayout() {
        if (!hasReadyComponents()) {
            return;
        }

        inputBar.setWidthFull();
        pendingContextLayout.setWidthFull();
        attachmentUpload.addClassName("ai-timeline-attachment-upload");
        addMenuBar.addClassName("ai-timeline-add-menu");
        attachmentUpload.getStyle().set("width", "auto");
        addMenuBar.getStyle().set("flex-shrink", "0");
        inputBar.expand(messageInput);
    }

    private void onReady(ReadyEvent event) {
        messageBundle.setMessageGroup("com.company.crm.ai.view.conversation");
        initComposer();
        refreshPendingContextLayout();
    }

    private void initComposer() {
        if (messageInput == null) {
            messageInput = uiComponents.create(MessageInput.class);
            messageInput.addSubmitListener(this::onMessageSubmit);
        }
        if (addMenuBar == null) {
            addMenuBar = addContextMenuFactory().createAddMenuBar(attachmentUpload);
        }

        applyVariantClassNames();

        inputBar.removeAll();
        inputBar.add(addMenuBar, messageInput);
        inputBar.expand(messageInput);
        messageInput.setEnabled(inputEnabled);
        applyVariantSpecificLayout();
    }

    @Subscribe("attachmentUpload")
    public void onAttachmentUploadSucceeded(final SucceededEvent event) {
        final TemporaryStorageFileData uploadedFileData = findUploadedFileData(event);

        if (uploadedFileData == null) {
            showMissingFileError();
            return;
        }

        final String uploadedFileName = resolveUploadedFileName(event, uploadedFileData);
        final FileRef uploadedFileRef = putFileIntoStorage(uploadedFileData, uploadedFileName);

        if (uploadedFileRef == null) {
            showMissingFileError();
            return;
        }

        try {
            var attachmentInput = new PendingAttachmentInput(uploadedFileRef, uploadedFileName);
            if (!attachments.contains(attachmentInput)) {
                attachments.add(attachmentInput);
            }
            attachmentUpload.clearFileList();
            refreshPendingContextLayout();
            focus();
        } catch (Exception e) {
            try {
                temporaryStorage.deleteFile(uploadedFileData.getFileInfo().getId());
            } catch (Exception cleanupError) {
                log.warn("Failed to cleanup temporary upload {}", uploadedFileData.getFileInfo().getId(), cleanupError);
            }
            attachmentUpload.clearFileList();
            log.error("Failed to stage uploaded attachment", e);
            notifications.create(messageBundle.getMessage("attachmentUploadPersistError"))
                    .withType(Notifications.Type.ERROR)
                    .show();
        }
    }

    private void showMissingFileError() {
        attachmentUpload.clearFileList();
        notifications.create(messageBundle.getMessage("attachmentUploadMissingFile"))
                .withType(Notifications.Type.ERROR)
                .show();
    }

    private String resolveUploadedFileName(SucceededEvent event, TemporaryStorageFileData uploadedFileData) {
        String fileNameFromEvent = event.getFileName();
        if (StringUtils.hasText(fileNameFromEvent)) {
            return fileNameFromEvent;
        }
        String fileNameFromBuffer = uploadedFileData.getFileName();
        if (StringUtils.hasText(fileNameFromBuffer)) {
            return fileNameFromBuffer;
        }
        return "uploaded-file";
    }

    private TemporaryStorageFileData findUploadedFileData(SucceededEvent event) {
        if (event.getUpload().getReceiver() instanceof MultiFileTemporaryStorageBuffer multiBuffer) {
            String fileName = event.getFileName();
            return multiBuffer.getFiles().values().stream()
                    .filter(data -> Objects.equals(data.getFileName(), fileName))
                    .filter(data -> data.getFileInfo().getFile().exists())
                    .findFirst()
                    .orElse(null);
        } else if (event.getUpload().getReceiver() instanceof FileTemporaryStorageBuffer storageBuffer) {
            return storageBuffer.getFileData();
        }
        return null;
    }

    private FileRef putFileIntoStorage(TemporaryStorageFileData uploadedFileData, String uploadedFileName) {
        try {
            FileStorage fileStorage = fileStorageLocator.getByName(CrmFileStorage.STORAGE_NAME);
            return temporaryStorage.putFileIntoStorage(
                    uploadedFileData.getFileInfo().getId(),
                    uploadedFileName,
                    fileStorage
            );
        } catch (Exception e) {
            log.warn("Failed to put file into storage", e);
            return null;
        }
    }

    @Subscribe("attachmentUpload")
    public void onAttachmentUploadFileRejected(final FileRejectedEvent event) {
        showAttachmentUploadError(event.getErrorMessage());
    }

    @Subscribe("attachmentUpload")
    public void onAttachmentUploadFailed(final FailedEvent event) {
        String reason = event.getReason() != null ? event.getReason().getMessage() : null;
        showAttachmentUploadError(reason);
    }

    private void showAttachmentUploadError(String detail) {
        attachmentUpload.clearFileList();
        final String baseMessage = messageBundle.getMessage("attachmentUploadRejected");
        final String message = StringUtils.hasText(detail)
                ? baseMessage + " (" + detail + ")"
                : baseMessage;
        notifications.create(message)
                .withType(Notifications.Type.WARNING)
                .show();
    }

    private void onMessageSubmit(MessageInput.SubmitEvent event) {
        submitHandler.accept(new Submission(
                event.getValue(),
                entityReferences(),
                attachments()
        ));
    }

    private void refreshPendingContextLayout() {
        if (pendingContextLayout == null) {
            return;
        }

        pendingContextLayout.removeAll();
        pendingContextLayout.setVisible(!isEmpty());

        if (!isEmpty()) {
            pendingContextLayout.add(contextCardFactory().createPendingContextCardsGrid(
                    entityReferences, entityRef -> {
                        removeEntityReference(entityRef);
                        focus();
                    },
                    attachments, attachment -> {
                        removeAttachment(attachment);
                        focus();
                    }
            ));
        }
    }

    private void openEntityLookup(String entityType, Class<?> entityClass) {
        dialogWindows.lookup(FragmentUtils.getHostView(this), entityClass)
                .withSelectHandler(selectedEntities -> selectedEntities.forEach(entity -> {
                    if (entity != null) {
                        try {
                            String entityRef = idSerialization.idToString(Id.of(entity));
                            if (!entityReferences.contains(entityRef)) {
                                entityReferences.add(entityRef);
                                refreshPendingContextLayout();
                                focus();
                            }
                        } catch (Exception e) {
                            log.error("Failed to add CRM entity to conversation context", e);
                            notifications.create(messageBundle.getMessage("errorProcessingMessage"))
                                    .withType(Notifications.Type.ERROR)
                                    .show();
                        }
                    }
                }))
                .open();
    }

    private AiConversationContextCardFactory contextCardFactory() {
        if (contextCardFactory == null) {
            contextCardFactory = new AiConversationContextCardFactory(
                    uiComponents,
                    metadata,
                    pendingAttachmentDownloadHandler,
                    pendingEntityReferenceOpenHandler
            );
        }
        return contextCardFactory;
    }

    private AddContextMenuFactory addContextMenuFactory() {
        if (addContextMenuFactory == null) {
            addContextMenuFactory = new AddContextMenuFactory(this::openEntityLookup, contextEntityRegistry, messageBundle);
        }
        return addContextMenuFactory;
    }

    private void applyVariantClassNames() {
        if (inputBar == null || pendingContextLayout == null) {
            return;
        }

        clearAllVariantClassNames();
        applyActiveVariantClassNames();
    }

    private void clearAllVariantClassNames() {
        Stream.of(Variant.values()).forEach(v -> {
            inputBar.removeClassName(v.getInputBarClass());
            pendingContextLayout.removeClassName(v.getPendingContextClass());
            if (messageInput != null && !v.getMessageInputClass().isEmpty()) {
                messageInput.removeClassName(v.getMessageInputClass());
            }
        });
    }

    private void applyActiveVariantClassNames() {
        inputBar.addClassName(variant.getInputBarClass());
        pendingContextLayout.addClassName(variant.getPendingContextClass());
        if (messageInput != null && !variant.getMessageInputClass().isEmpty()) {
            messageInput.addClassName(variant.getMessageInputClass());
        }
    }

    private void applyVariantSpecificLayout() {
        if (variant == Variant.TIMELINE) {
            configureTimelineLayout();
        }
    }

    private boolean hasReadyComponents() {
        return inputBar != null
                && pendingContextLayout != null
                && attachmentUpload != null
                && addMenuBar != null
                && messageInput != null;
    }


    public enum Variant {
        STARTER("ai-conversation-starter-input-bar", "ai-conversation-starter-pending-context", "ai-conversation-starter-message-input"),
        TIMELINE("ai-timeline-input-bar", "ai-timeline-pending-context", "");

        private final String inputBarClass;
        private final String pendingContextClass;
        private final String messageInputClass;

        Variant(String inputBarClass, String pendingContextClass, String messageInputClass) {
            this.inputBarClass = inputBarClass;
            this.pendingContextClass = pendingContextClass;
            this.messageInputClass = messageInputClass;
        }

        public String getInputBarClass() {
            return inputBarClass;
        }

        public String getPendingContextClass() {
            return pendingContextClass;
        }

        public String getMessageInputClass() {
            return messageInputClass;
        }
    }

    public record Submission(String prompt,
                             List<String> entityReferences,
                             List<PendingAttachmentInput> attachments) {
    }
}
