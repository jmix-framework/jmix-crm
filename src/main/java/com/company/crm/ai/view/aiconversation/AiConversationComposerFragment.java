package com.company.crm.ai.view.aiconversation;

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
import io.jmix.core.DataManager;
import io.jmix.core.IdSerialization;
import io.jmix.core.MessageTools;
import io.jmix.core.Metadata;
import io.jmix.core.MetadataTools;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@FragmentDescriptor("ai-conversation-composer-fragment.xml")
public class AiConversationComposerFragment extends Fragment<VerticalLayout> {

    private static final Logger log = LoggerFactory.getLogger(AiConversationComposerFragment.class);

    @ViewComponent
    private HorizontalLayout inputBar;
    @ViewComponent
    private VerticalLayout pendingContextLayout;
    @ViewComponent
    private JmixUpload attachmentUpload;
    @ViewComponent
    private MessageBundle messageBundle;

    @Autowired
    private DataManager dataManager;
    @Autowired
    private IdSerialization idSerialization;
    @Autowired
    private Metadata metadata;
    @Autowired
    private MetadataTools metadataTools;
    @Autowired
    private MessageTools messageTools;
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
    private Consumer<Submission> submitHandler = submission -> {
    };
    private Consumer<AiConversationAttachment> pendingAttachmentDownloadHandler = attachment -> {
    };
    private Consumer<String> pendingEntityReferenceOpenHandler = entityReference -> {
    };
    private Variant variant = Variant.TIMELINE;

    public AiConversationComposerFragment() {
        addReadyListener(this::onReady);
    }

    public void setVariant(Variant variant) {
        this.variant = Objects.requireNonNull(variant);
        applyVariantClassNames();
    }

    public void setSubmitHandler(Consumer<Submission> submitHandler) {
        this.submitHandler = submitHandler != null ? submitHandler : submission -> {
        };
    }

    public void setPendingContextActions(Consumer<AiConversationAttachment> pendingAttachmentDownloadHandler,
                                         Consumer<String> pendingEntityReferenceOpenHandler) {
        this.pendingAttachmentDownloadHandler = pendingAttachmentDownloadHandler != null
                ? pendingAttachmentDownloadHandler
                : attachment -> {
        };
        this.pendingEntityReferenceOpenHandler = pendingEntityReferenceOpenHandler != null
                ? pendingEntityReferenceOpenHandler
                : entityReference -> {
        };
        contextCardFactory = null;
        refreshPendingContextLayout();
    }

    public void setInputEnabled(boolean enabled) {
        if (messageInput != null) {
            messageInput.setEnabled(enabled);
        }
    }

    public void focus() {
        if (messageInput == null) {
            return;
        }

        messageInput.getElement().executeJs(
                "requestAnimationFrame(() => {"
                        + "  const target = this.shadowRoot && this.shadowRoot.querySelector('vaadin-text-area');"
                        + "  (target || this).focus();"
                        + "});"
        );
    }

    public void clear() {
        entityReferences.clear();
        attachments.clear();
        refreshPendingContextLayout();
    }

    public void addEntityReferences(Collection<String> refs) {
        if (refs == null || refs.isEmpty()) {
            return;
        }
        for (String ref : refs) {
            if (!entityReferences.contains(ref)) {
                entityReferences.add(ref);
            }
        }
        refreshPendingContextLayout();
    }

    public void addAttachments(Collection<PendingAttachmentInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return;
        }
        for (PendingAttachmentInput input : inputs) {
            if (!attachments.contains(input)) {
                attachments.add(input);
            }
        }
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

    MessageInput getMessageInput() {
        return messageInput;
    }

    HorizontalLayout getInputBar() {
        return inputBar;
    }

    VerticalLayout getPendingContextLayout() {
        return pendingContextLayout;
    }

    JmixUpload getAttachmentUpload() {
        return attachmentUpload;
    }

    MenuBar getAddMenuBar() {
        return addMenuBar;
    }

    private void onReady(ReadyEvent event) {
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
    }

    @Subscribe("attachmentUpload")
    public void onAttachmentUploadSucceeded(final SucceededEvent event) {
        TemporaryStorageFileData uploadedFileData = null;
        if (event.getUpload().getReceiver() instanceof MultiFileTemporaryStorageBuffer multiBuffer) {
            String fileName = event.getFileName();
            uploadedFileData = multiBuffer.getFiles().values().stream()
                    .filter(data -> Objects.equals(data.getFileName(), fileName))
                    .filter(data -> data.getFileInfo().getFile().exists())
                    .findFirst()
                    .orElse(null);
        } else if (event.getUpload().getReceiver() instanceof FileTemporaryStorageBuffer storageBuffer) {
            uploadedFileData = storageBuffer.getFileData();
        }

        if (uploadedFileData == null) {
            attachmentUpload.clearFileList();
            notifications.create(messageBundle.getMessage("attachmentUploadMissingFile"))
                    .withType(Notifications.Type.ERROR)
                    .show();
            return;
        }

        String fileNameFromEvent = event.getFileName();
        String fileNameFromBuffer = uploadedFileData.getFileName();
        String uploadedFileName = "uploaded-file";
        if (fileNameFromEvent != null && !fileNameFromEvent.isBlank()) {
            uploadedFileName = fileNameFromEvent;
        } else if (fileNameFromBuffer != null && !fileNameFromBuffer.isBlank()) {
            uploadedFileName = fileNameFromBuffer;
        }

        FileRef uploadedFileRef = null;
        try {
            FileStorage fileStorage = fileStorageLocator.getByName(CrmFileStorage.STORAGE_NAME);
            uploadedFileRef = temporaryStorage.putFileIntoStorage(
                    uploadedFileData.getFileInfo().getId(),
                    uploadedFileName,
                    fileStorage
            );
        } catch (Exception e) {
            log.warn("Failed to put file into storage", e);
        }

        if (uploadedFileRef == null) {
            attachmentUpload.clearFileList();
            notifications.create(messageBundle.getMessage("attachmentUploadMissingFile"))
                    .withType(Notifications.Type.ERROR)
                    .show();
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
            if (uploadedFileData != null) {
                try {
                    temporaryStorage.deleteFile(uploadedFileData.getFileInfo().getId());
                } catch (Exception cleanupError) {
                    log.warn("Failed to cleanup temporary upload {}", uploadedFileData.getFileInfo().getId(), cleanupError);
                }
            }
            attachmentUpload.clearFileList();
            log.error("Failed to stage uploaded attachment", e);
            notifications.create(messageBundle.getMessage("attachmentUploadPersistError"))
                    .withType(Notifications.Type.ERROR)
                    .show();
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
        String message = messageBundle.getMessage("attachmentUploadRejected");
        if (detail != null && !detail.isBlank()) {
            message = message + " (" + detail + ")";
        }
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

        if (!entityReferences.isEmpty()) {
            pendingContextLayout.add(contextCardFactory().createPendingEntityReferenceCardsGrid(entityReferences, entityRef -> {
                removeEntityReference(entityRef);
                focus();
            }));
        }
        if (!attachments.isEmpty()) {
            pendingContextLayout.add(contextCardFactory().createPendingAttachmentCardsGrid(attachments, attachment -> {
                removeAttachment(attachment);
                focus();
            }));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
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
                    dataManager,
                    idSerialization,
                    metadata,
                    metadataTools,
                    messageTools,
                    messageBundle,
                    pendingAttachmentDownloadHandler,
                    pendingEntityReferenceOpenHandler,
                    contextEntityRegistry
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

        inputBar.removeClassName("ai-conversation-starter-input-bar");
        inputBar.removeClassName("ai-timeline-input-bar");
        pendingContextLayout.removeClassName("ai-conversation-starter-pending-context");
        pendingContextLayout.removeClassName("ai-timeline-pending-context");
        if (messageInput != null) {
            messageInput.removeClassName("ai-conversation-starter-message-input");
        }

        if (Variant.STARTER.equals(variant)) {
            inputBar.addClassName("ai-conversation-starter-input-bar");
            pendingContextLayout.addClassName("ai-conversation-starter-pending-context");
            if (messageInput != null) {
                messageInput.addClassName("ai-conversation-starter-message-input");
            }
        } else {
            inputBar.addClassName("ai-timeline-input-bar");
            pendingContextLayout.addClassName("ai-timeline-pending-context");
        }
    }

    public enum Variant {
        STARTER,
        TIMELINE
    }

    public record Submission(String prompt,
                             List<String> entityReferences,
                             List<PendingAttachmentInput> attachments) {
    }
}
