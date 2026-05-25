package com.company.crm.ai.view.aiconversation;

import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class AiConversationTimeFormatter {

    private final DatatypeFormatter datatypeFormatter;

    public AiConversationTimeFormatter(DatatypeFormatter datatypeFormatter) {
        this.datatypeFormatter = datatypeFormatter;
    }

    public String format(OffsetDateTime createdDate) {
        return datatypeFormatter.formatOffsetDateTime(createdDate);
    }
}
