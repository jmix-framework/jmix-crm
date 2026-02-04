package com.company.crm.model.datatype;

import com.company.crm.app.util.context.AppContext;
import io.jmix.core.metamodel.annotation.DatatypeDef;
import io.jmix.core.metamodel.annotation.Ddl;
import io.jmix.core.metamodel.datatype.Datatype;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Locale;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;

@DatatypeDef(id = PriceDataType.NAME, javaClass = BigDecimal.class)
@Ddl("DECIMAL(19,2)")
public class PriceDataType implements Datatype<BigDecimal> {

    public static final String NAME = "price";
    private static final CurrencyPosition DEFAULT_CURRENCY_POSITION = CurrencyPosition.START;

    public static String formatWithoutCurrency(Object value) {
        return doFormatValueWithoutCurrency(value);
    }

    public static String defaultFormat(Object value) {
        return doFormatValueWithCurrency(value, DEFAULT_CURRENCY_POSITION);
    }

    public static String formatStartingCurrency(Object value) {
        return doFormatValueWithCurrency(value, CurrencyPosition.START);
    }

    public static String formatEndingCurrency(Object value) {
        return doFormatValueWithCurrency(value, CurrencyPosition.END);
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
        value = defaultIfBlank(substringBefore(value, getCurrencySymbol()), value);
        value = defaultIfBlank(substringAfter(value, getCurrencySymbol()), value);
        value = StringUtils.trim(value);

        if (StringUtils.isBlank(value)) {
            return null;
        }

        try {
            BigDecimal price = getDatatypeFormatter().parseBigDecimal(value);
            return (price == null || price.compareTo(BigDecimal.ZERO) < 0) ? BigDecimal.ZERO : price;
        } catch (ParseException e) {
            return BigDecimal.ZERO;
        }
    }

    @Nullable
    @Override
    public BigDecimal parse(@Nullable String value, Locale locale) {
        return parse(value);
    }

    public static String getCurrencySymbol() {
        return "$";
    }

    private static String doFormatValueWithCurrency(Object value, CurrencyPosition currencyPosition) {
        String withoutCurrency = formatWithoutCurrency(value);
        return switch (currencyPosition) {
            case START -> getCurrencySymbol() + withoutCurrency;
            case END -> withoutCurrency + getCurrencySymbol();
        };
    }

    private static String doFormatValueWithoutCurrency(Object value) {
        if (value == null) {
            return "";
        }

        BigDecimal decimalValue = resolveBigDecimalValue(value);
        if (decimalValue != null) {
            return getDatatypeFormatter().formatBigDecimal(decimalValue);
        }

        return "[NaN]";
    }

    @Nullable
    private static BigDecimal resolveBigDecimalValue(Object value) {
        BigDecimal decimalValue;
        if (value instanceof BigDecimal decimal) {
            decimalValue = decimal;
        } else if (value instanceof String string) {
            try {
                decimalValue = new BigDecimal(string);
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            throw new IllegalStateException("Unsupported value type for price formatting: " + value.getClass().getName());
        }
        return decimalValue;
    }

    private static DatatypeFormatter getDatatypeFormatter() {
        return AppContext.getBean(DatatypeFormatter.class);
    }
}
