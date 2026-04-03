package com.company.crm.model.datatype;

import com.company.crm.app.util.context.AppContext;
import io.jmix.core.Messages;
import io.jmix.core.metamodel.annotation.DatatypeDef;
import io.jmix.core.metamodel.annotation.Ddl;
import io.jmix.core.metamodel.datatype.Datatype;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import io.jmix.core.security.CurrentAuthentication;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
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

    public static String formatWithoutCurrency(Object value, DatatypeFormatter datatypeFormatter) {
        return doFormatValueWithoutCurrency(value, datatypeFormatter);
    }

    public static String defaultFormat(Object value, DatatypeFormatter datatypeFormatter) {
        return doFormatValueWithCurrency(value, datatypeFormatter, null);
    }

    public enum CurrencyPosition {
        START, END
    }

    @Override
    public String format(@Nullable Object value) {
        return doFormatValueWithCurrency(value, getDatatypeFormatter(), null);
    }

    @Override
    public String format(@Nullable Object value, Locale locale) {
        return doFormatValueWithCurrency(value, getDatatypeFormatter(), locale);
    }

    @Nullable
    @Override
    public BigDecimal parse(@Nullable String value) {
        return parse(value, null);
    }

    @Nullable
    @Override
    public BigDecimal parse(@Nullable String value, Locale locale) {
        String currencySymbol = getCurrencySymbol(locale);
        value = defaultIfBlank(substringBefore(value, currencySymbol), value);
        value = defaultIfBlank(substringAfter(value, currencySymbol), value);
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

    private static @NonNull DatatypeFormatter getDatatypeFormatter() {
        return AppContext.getBean(DatatypeFormatter.class);
    }

    public static String getCurrencySymbol() {
        return getCurrencySymbol(currentLocale());
    }

    public static String getCurrencySymbol(@Nullable Locale locale) {
        Locale effectiveLocale = locale != null ? locale : currentLocale();
        return getMessages().getMessage("currencySymbol", effectiveLocale);
    }

    private static @NonNull Messages getMessages() {
        return AppContext.getBean(Messages.class);
    }

    public static CurrencyPosition getCurrencyPosition(@Nullable Locale locale) {
        Locale effectiveLocale = locale != null ? locale : currentLocale();
        String configuredPosition = getMessages().getMessage("currencyPosition", effectiveLocale);

        try {
            return CurrencyPosition.valueOf(configuredPosition.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return DEFAULT_CURRENCY_POSITION;
        }
    }

    private static String doFormatValueWithCurrency(Object value,
                                                    DatatypeFormatter datatypeFormatter,
                                                    @Nullable Locale locale) {
        String withoutCurrency = formatWithoutCurrency(value, datatypeFormatter);
        return switch (getCurrencyPosition(locale)) {
            case START -> getCurrencySymbol(locale) + withoutCurrency;
            case END -> withoutCurrency + getCurrencySymbol(locale);
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

    private static Locale currentLocale() {
        return AppContext.getBean(CurrentAuthentication.class).getLocale();
    }
}
