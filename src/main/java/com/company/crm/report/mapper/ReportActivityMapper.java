package com.company.crm.report.mapper;

import com.company.crm.model.user.activity.client.ClientUserActivity;
import java.util.Objects;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapper component for converting ClientUserActivity entities to report data maps.
 * Handles consistent formatting and null safety for activity report data.
 */
@Component
public class ReportActivityMapper {

    private final DatatypeFormatter datatypeFormatter;

    public ReportActivityMapper(DatatypeFormatter datatypeFormatter) {
        this.datatypeFormatter = datatypeFormatter;
    }

    /**
     * Converts a ClientUserActivity entity to a Map suitable for report data binding.
     *
     * @param activity The activity to convert
     * @return Map containing formatted activity data fields
     */
    public Map<String, Object> toReportMap(ClientUserActivity activity) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("description", formatDescription(activity));
        fields.put("createdDate", activity.getCreatedDate());
        fields.put("createdDateFormatted", activity.getCreatedDate() != null ?
                datatypeFormatter.formatLocalDateTime(activity.getCreatedDate().toLocalDateTime()) : "");
        fields.put("user", formatUser(activity));
        return fields;
    }

    /**
     * Formats the activity description with meaningful fallbacks for empty descriptions.
     *
     * @param activity The activity to get description from
     * @return Formatted description with fallback for null/empty values
     */
    private String formatDescription(ClientUserActivity activity) {
        String description = activity.getActionDescription();
        if (description == null || description.trim().isEmpty()) {
            return "Customer interaction recorded";
        }
        return description;
    }


    /**
     * Formats the user name with fallback for system activities.
     *
     * @param activity The activity to get user from
     * @return User name or "System" if user is null
     */
    private String formatUser(ClientUserActivity activity) {
        return activity.getUser() != null ?
                Objects.toString(activity.getUser().getUsername(), "") : "System";
    }
}