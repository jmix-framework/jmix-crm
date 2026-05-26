package com.company.crm.ai.service;

import com.vaadin.flow.component.icon.VaadinIcon;

public enum AiAttachmentMediaKind {
    SPREADSHEET(VaadinIcon.TABLE),
    TEXT_DOCUMENT(VaadinIcon.FILE_TEXT_O),
    JSON(VaadinIcon.CODE),
    IMAGE(VaadinIcon.FILE_O),
    OTHER(VaadinIcon.FILE_O);

    private final VaadinIcon icon;

    AiAttachmentMediaKind(VaadinIcon icon) {
        this.icon = icon;
    }

    public VaadinIcon getIcon() {
        return icon;
    }
}
