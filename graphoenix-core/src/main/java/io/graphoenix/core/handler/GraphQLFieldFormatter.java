package io.graphoenix.core.handler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Formatter;
import java.util.Locale;

import static jakarta.json.JsonValue.FALSE;
import static jakarta.json.JsonValue.TRUE;
import static jakarta.json.JsonValue.NULL;

@ApplicationScoped
public class GraphQLFieldFormatter {

    private final JsonProvider jsonProvider;

    @Inject
    public GraphQLFieldFormatter(JsonProvider jsonProvider) {
        this.jsonProvider = jsonProvider;
    }

    public JsonValue format(String value, String locale, LocalDateTime localDateTime) throws ClassCastException {
        if (localDateTime == null) {
            return NULL;
        }
        if (value != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(value, locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale));
            return jsonProvider.createValue(localDateTime.format(formatter));
        } else {
            return jsonProvider.createValue(localDateTime.toString());
        }
    }

    public JsonValue format(String value, String locale, LocalDate localDate) throws ClassCastException {
        if (localDate == null) {
            return NULL;
        }
        if (value != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(value, locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale));
            return jsonProvider.createValue(localDate.format(formatter));
        } else {
            return jsonProvider.createValue(localDate.toString());
        }
    }

    public JsonValue format(String value, String locale, LocalTime localTime) throws ClassCastException {
        if (localTime == null) {
            return NULL;
        }
        if (value != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(value, locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale));
            return jsonProvider.createValue(localTime.format(formatter));
        } else {
            return jsonProvider.createValue(localTime.toString());
        }
    }

    public JsonValue format(String value, String locale, Integer number) throws ClassCastException {
        if (number == null) {
            return NULL;
        }
        if (value != null) {
            DecimalFormat decimalFormat = new DecimalFormat(value);
            decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale)));
            return jsonProvider.createValue(decimalFormat.format(number));
        } else {
            return jsonProvider.createValue(number);
        }
    }

    public JsonValue format(String value, String locale, Long number) throws ClassCastException {
        if (number == null) {
            return NULL;
        }
        if (value != null) {
            DecimalFormat decimalFormat = new DecimalFormat(value);
            decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale)));
            return jsonProvider.createValue(decimalFormat.format(number));
        } else {
            return jsonProvider.createValue(number);
        }
    }

    public JsonValue format(String value, String locale, Double number) throws ClassCastException {
        if (number == null) {
            return NULL;
        }
        if (value != null) {
            DecimalFormat decimalFormat = new DecimalFormat(value);
            decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale)));
            return jsonProvider.createValue(decimalFormat.format(number));
        } else {
            return jsonProvider.createValue(number);
        }
    }

    public JsonValue format(String value, String locale, Float number) throws ClassCastException {
        if (number == null) {
            return NULL;
        }
        if (value != null) {
            DecimalFormat decimalFormat = new DecimalFormat(value);
            decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale)));
            return jsonProvider.createValue(decimalFormat.format(number));
        } else {
            return jsonProvider.createValue(number);
        }
    }

    public JsonValue format(String value, String locale, BigInteger number) throws ClassCastException {
        if (number == null) {
            return NULL;
        }
        if (value != null) {
            DecimalFormat decimalFormat = new DecimalFormat(value);
            decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale)));
            return jsonProvider.createValue(decimalFormat.format(number));
        } else {
            return jsonProvider.createValue(number);
        }
    }

    public JsonValue format(String value, String locale, BigDecimal number) throws ClassCastException {
        if (number == null) {
            return NULL;
        }
        if (value != null) {
            DecimalFormat decimalFormat = new DecimalFormat(value);
            decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale)));
            return jsonProvider.createValue(decimalFormat.format(number));
        } else {
            return jsonProvider.createValue(number);
        }
    }

    public JsonValue format(String value, String locale, String string) throws ClassCastException {
        if (string == null) {
            return NULL;
        }
        if (value != null) {
            Formatter formatter = new Formatter(locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale));
            return jsonProvider.createValue(formatter.format(value, string).toString());
        } else {
            return jsonProvider.createValue(string);
        }
    }

    public JsonValue format(String value, String locale, Boolean bool) throws ClassCastException {
        if (bool == null) {
            return NULL;
        }
        if (value != null) {
            Formatter formatter = new Formatter(locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale));
            return jsonProvider.createValue(formatter.format(value, bool).toString());
        } else {
            return bool ? TRUE : FALSE;
        }
    }

    public JsonValue format(String value, String locale, Enum<?> enumValue) throws ClassCastException {
        if (enumValue == null) {
            return NULL;
        }
        if (value != null) {
            Formatter formatter = new Formatter(locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale));
            return jsonProvider.createValue(formatter.format(value, enumValue.name()).toString());
        } else {
            return jsonProvider.createValue(enumValue.name());
        }
    }
}
