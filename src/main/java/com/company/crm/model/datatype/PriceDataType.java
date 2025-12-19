package com.company.crm.model.datatype;

import io.jmix.core.metamodel.annotation.DatatypeDef;
import io.jmix.core.metamodel.annotation.Ddl;
import io.jmix.core.metamodel.datatype.Datatype;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

@DatatypeDef(id = PriceDataType.NAME, javaClass = BigDecimal.class)
@Ddl("DECIMAL(19,2)")
public class PriceDataType implements Datatype<BigDecimal> {

    public static final String NAME = "price";

    private static final String PATTERN = "#,##0";
    private static final CurrencyPosition DEFAULT_CURRENCY_POSITION = CurrencyPosition.END;

    public static String formatWithoutCurrency(Object value) {
        return doFormatValueWithoutCurrency(value);
    }

    public static String formatStartingCurrency(Object value) {
        return doFormatValueWithCurrency(value, CurrencyPosition.START);
    }

    public static String formatEndingCurrency(Object value) {
        return doFormatValueWithCurrency(value, CurrencyPosition.END);
    }

    private static String doFormatValueWithCurrency(Object value, CurrencyPosition currencyPosition) {
        String withoutCurrency = formatWithoutCurrency(value);
        return switch (currencyPosition) {
            case START -> getCurrencyPrefix() + withoutCurrency;
            case END -> withoutCurrency + getCurrencySuffix();
        };
    }

    private static String doFormatValueWithoutCurrency(Object value) {
        if (value == null) {
            return "";
        }
        try {
            final NumberFormat numberInstance = NumberFormat.getNumberInstance();
            DecimalFormat decimalFormat = (DecimalFormat) numberInstance;
            decimalFormat.setParseBigDecimal(true);
            decimalFormat.applyPattern(PATTERN);
            return decimalFormat.format(value);
        } catch (Exception e) {
            return "[NaN]";
        }
    }

    public enum CurrencyPosition {
        START, END
    }

    @Override
    public String format(@Nullable Object value) {
        return doFormatValueWithCurrency(value, DEFAULT_CURRENCY_POSITION);
    }

    @Override
    public String format(@Nullable Object value, Locale locale) {
        return format(value);
    }

    @Nullable
    @Override
    public BigDecimal parse(@Nullable String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        final NumberFormat numberInstance = NumberFormat.getNumberInstance();
        DecimalFormat decimalFormat = (DecimalFormat) numberInstance;
        decimalFormat.setParseBigDecimal(true);

        try {
            BigDecimal price = ((BigDecimal) decimalFormat.parse(value)).setScale(0, RoundingMode.DOWN);
            return price.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : price;
        } catch (ParseException e) {
            return BigDecimal.ZERO;
        }
    }

    @Nullable
    @Override
    public BigDecimal parse(@Nullable String value, Locale locale) {
        return parse(value);
    }

    public static String getCurrencySuffix() {
        return " " + getCurrencySymbol();
    }

    public static String getCurrencyPrefix() {
        return getCurrencySymbol() + " ";
    }

    public static String getCurrencySymbol() {
        return "$";
    }
}
