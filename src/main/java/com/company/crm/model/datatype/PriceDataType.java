package com.company.crm.model.datatype;

import io.jmix.core.metamodel.annotation.DatatypeDef;
import io.jmix.core.metamodel.annotation.Ddl;
import io.jmix.core.metamodel.datatype.Datatype;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Locale;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;

@DatatypeDef(id = PriceDataType.NAME, javaClass = BigDecimal.class)
@Ddl("DECIMAL(19,2)")
public class PriceDataType implements Datatype<BigDecimal> {

    @Lazy
    @Autowired
    private DatatypeFormatter datatypeFormatter;

    public static final String NAME = "price";
    private static final CurrencyPosition DEFAULT_CURRENCY_POSITION = CurrencyPosition.START;

    public static String formatWithoutCurrency(Object value, DatatypeFormatter datatypeFormatter) {
        return doFormatValueWithoutCurrency(value, datatypeFormatter);
    }

    public static String defaultFormat(Object value, DatatypeFormatter datatypeFormatter) {
        return doFormatValueWithCurrency(value, datatypeFormatter, DEFAULT_CURRENCY_POSITION);
    }

    public enum CurrencyPosition {
        START, END
    }

    @Override
    public String format(@Nullable Object value) {
        return doFormatValueWithCurrency(value, datatypeFormatter, DEFAULT_CURRENCY_POSITION);
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
            BigDecimal price = datatypeFormatter.parseBigDecimal(value);
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

    private static String doFormatValueWithCurrency(Object value,
                                                    DatatypeFormatter datatypeFormatter,
                                                    CurrencyPosition currencyPosition) {
        String withoutCurrency = formatWithoutCurrency(value, datatypeFormatter);
        return switch (currencyPosition) {
            case START -> getCurrencySymbol() + withoutCurrency;
            case END -> withoutCurrency + getCurrencySymbol();
        };
    }

    private static String doFormatValueWithoutCurrency(Object value,
                                                       DatatypeFormatter datatypeFormatter) {
        if (value == null) {
            return "";
        }

        BigDecimal decimalValue = resolveBigDecimalValue(value);
        if (decimalValue != null) {
            return datatypeFormatter.formatBigDecimal(decimalValue);
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
}
