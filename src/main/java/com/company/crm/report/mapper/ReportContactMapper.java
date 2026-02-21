package com.company.crm.report.mapper;

import com.company.crm.model.contact.Contact;
import java.util.Objects;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapper component for converting Contact entities to report data maps.
 * Handles consistent formatting and null safety for contact report data.
 */
@Component
public class ReportContactMapper {

    private final DatatypeFormatter datatypeFormatter;

    public ReportContactMapper(DatatypeFormatter datatypeFormatter) {
        this.datatypeFormatter = datatypeFormatter;
    }

    /**
     * Converts a Contact entity to a Map suitable for report data binding.
     *
     * @param contact The contact to convert
     * @return Map containing formatted contact data fields
     */
    public Map<String, Object> toReportMap(Contact contact) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("person", Objects.toString(contact.getPerson(), ""));
        fields.put("position", Objects.toString(contact.getPosition(), ""));
        fields.put("phone", Objects.toString(contact.getPhone(), ""));
        fields.put("email", Objects.toString(contact.getEmail(), ""));
        fields.put("startDate", contact.getStartDate());
        fields.put("startDateFormatted", datatypeFormatter.formatLocalDate(contact.getStartDate()));
        fields.put("endDate", contact.getEndDate());
        fields.put("endDateFormatted", datatypeFormatter.formatLocalDate(contact.getEndDate()));
        fields.put("isActive", isActiveContact(contact));
        return fields;
    }


    /**
     * Determines if a contact is currently active based on end date.
     *
     * @param contact The contact to check
     * @return true if contact is active (end date is null or in the future)
     */
    private boolean isActiveContact(Contact contact) {
        return contact.getEndDate() == null || contact.getEndDate().isAfter(LocalDate.now());
    }
}