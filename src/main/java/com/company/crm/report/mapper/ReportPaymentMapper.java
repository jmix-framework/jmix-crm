package com.company.crm.report.mapper;

import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.payment.Payment;
import java.util.Objects;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapper component for converting Payment entities to report data maps.
 * Handles consistent formatting and null safety for payment report data.
 */
@Component
public class ReportPaymentMapper {

    private final DatatypeFormatter datatypeFormatter;

    public ReportPaymentMapper(DatatypeFormatter datatypeFormatter) {
        this.datatypeFormatter = datatypeFormatter;
    }

    /**
     * Converts a Payment entity to a Map suitable for report data binding.
     *
     * @param payment The payment to convert
     * @return Map containing formatted payment data fields
     */
    public Map<String, Object> toReportMap(Payment payment) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("number", Objects.toString(payment.getNumber(), ""));
        fields.put("date", payment.getDate());
        fields.put("dateFormatted", datatypeFormatter.formatLocalDate(payment.getDate()));
        fields.put("amount", PriceDataType.defaultFormat(payment.getAmount()));
        fields.put("invoiceNumber", extractInvoiceNumber(payment));
        return fields;
    }


    /**
     * Safely extracts the invoice number from a payment's associated invoice.
     *
     * @param payment The payment to extract invoice number from
     * @return Invoice number string or empty string if invoice or number is null
     */
    private String extractInvoiceNumber(Payment payment) {
        return payment.getInvoice() != null ?
                Objects.toString(payment.getInvoice().getNumber(), "") : "";
    }
}