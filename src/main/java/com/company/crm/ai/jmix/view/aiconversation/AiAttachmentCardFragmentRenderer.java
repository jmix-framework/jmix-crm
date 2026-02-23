package com.company.crm.ai.jmix.view.aiconversation;

import com.company.crm.ai.entity.AiConversationAttachment;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import io.jmix.flowui.component.card.JmixCard;
import io.jmix.flowui.download.Downloader;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.fragmentrenderer.FragmentRenderer;
import io.jmix.flowui.fragmentrenderer.RendererItemContainer;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.MessageBundle;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewComponent;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Locale;

@FragmentDescriptor("ai-attachment-card-fragment.xml")
@RendererItemContainer("attachmentDc")
public class AiAttachmentCardFragmentRenderer extends FragmentRenderer<JmixCard, AiConversationAttachment> {

    @ViewComponent
    private InstanceContainer<AiConversationAttachment> attachmentDc;
    @ViewComponent
    private Icon attachmentIcon;
    @ViewComponent
    private H5 titleValue;
    @ViewComponent
    private Span timestampValue;
    @ViewComponent
    private JmixButton openButton;
    @ViewComponent
    private MessageBundle messageBundle;

    @Autowired
    private Downloader downloader;

    @Subscribe
    public void onReady(final ReadyEvent event) {
        openButton.setAriaLabel(messageBundle.getMessage("attachmentsDownloadAction"));
        applyAttachmentState(attachmentDc.getItemOrNull());
    }

    @Subscribe(id = "attachmentDc", target = Target.DATA_CONTAINER)
    public void onAttachmentDcItemChange(final InstanceContainer.ItemChangeEvent<AiConversationAttachment> event) {
        applyAttachmentState(event.getItem());
    }

    @Subscribe("openButton")
    public void onOpenButtonClick(final ClickEvent<JmixButton> event) {
        AiConversationAttachment attachment = attachmentDc.getItemOrNull();
        if (attachment == null || attachment.getFile() == null) {
            return;
        }
        downloader.setShowNewWindow(true);
        downloader.download(attachment.getFile());
    }

    private void applyAttachmentState(AiConversationAttachment attachment) {
        if (attachment == null) {
            titleValue.setText(messageBundle.getMessage("attachmentsMissingFileName"));
            attachmentIcon.setIcon(VaadinIcon.FILE_O);
            openButton.setEnabled(false);
            return;
        }

        String title = attachment.getTitle() != null && !attachment.getTitle().isBlank()
                ? attachment.getTitle()
                : (attachment.getFileName() != null && !attachment.getFileName().isBlank()
                ? attachment.getFileName()
                : messageBundle.getMessage("attachmentsMissingFileName"));
        titleValue.setText(title);

        attachmentIcon.setIcon(resolveIcon(attachment));
        openButton.setEnabled(attachment.getFile() != null);
    }

    private VaadinIcon resolveIcon(AiConversationAttachment attachment) {
        String extension = fileExtension(attachment.getFileName());
        return switch (extension) {
            case "csv", "xlsx", "xls" -> VaadinIcon.TABLE;
            case "pdf", "html", "htm", "md", "txt" -> VaadinIcon.FILE_TEXT_O;
            default -> VaadinIcon.FILE_O;
        };
    }

    private String fileExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
