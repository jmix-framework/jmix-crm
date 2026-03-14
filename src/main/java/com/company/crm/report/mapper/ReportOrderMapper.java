package com.company.crm.report.mapper;

import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.order.Order;
import java.util.Objects;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapper component for converting Order entities to report data maps.
 * Handles consistent formatting and null safety for order report data.
 */
@Component
public class ReportOrderMapper {

    private final DatatypeFormatter datatypeFormatter;
    private final MetadataTools metadataTools;

    public ReportOrderMapper(DatatypeFormatter datatypeFormatter, MetadataTools metadataTools) {
        this.datatypeFormatter = datatypeFormatter;
        this.metadataTools = metadataTools;
    }

    /**
     * Converts an Order entity to a Map suitable for report data binding.
     *
     * @param order The order to convert
     * @return Map containing formatted order data fields
     */
    public Map<String, Object> toReportMap(Order order) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("number", Objects.toString(order.getNumber(), ""));
        fields.put("date", order.getDate());
        fields.put("dateFormatted", datatypeFormatter.formatLocalDate(order.getDate()));
        fields.put("status", metadataTools.format(order.getStatus()));
        fields.put("total", PriceDataType.defaultFormat(order.getTotal(), datatypeFormatter));
        fields.put("comment", Objects.toString(order.getComment(), ""));
        return fields;
    }

}