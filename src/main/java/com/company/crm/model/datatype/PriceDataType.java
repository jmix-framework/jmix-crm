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

    public static final PriceDataType INSTANCE = new PriceDataType();

    public static String formatValue(BigDecimal value) {
        return INSTANCE.format(value);
    }

    @Override
    public String format(@Nullable Object value) {
        if (value == null) {
            return "";
        }

        try {
            final NumberFormat numberInstance = NumberFormat.getNumberInstance();
            DecimalFormat decimalFormat = (DecimalFormat) numberInstance;
            decimalFormat.setParseBigDecimal(true);
            decimalFormat.applyPattern(PATTERN);

            return decimalFormat.format(value) + getCurrencyPostfix();
        } catch (Exception e) {
            return value.toString();
        }
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

    private String getCurrencyPostfix() {
        return " " + "$";
    }
}
