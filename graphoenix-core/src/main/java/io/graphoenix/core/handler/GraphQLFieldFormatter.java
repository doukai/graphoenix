package io.graphoenix.core.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@ApplicationScoped
public class GraphQLFieldFormatter {

    public void format(JsonObject jsonObject, String selectionName, String value, String locale, LocalDateTime localDateTime) throws ClassCastException {
        if (value != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(value, locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale));
            jsonObject.addProperty(selectionName, localDateTime.format(formatter));
        } else {
            jsonObject.addProperty(selectionName, localDateTime.toString());
        }
    }

    public void format(JsonObject jsonObject, String selectionName, String value, String locale, LocalDate localDate) throws ClassCastException {
        if (value != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(value, locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale));
            jsonObject.addProperty(selectionName, localDate.format(formatter));
        } else {
            jsonObject.addProperty(selectionName, localDate.toString());
        }
    }

    public void format(JsonObject jsonObject, String selectionName, String value, String locale, LocalTime localTime) throws ClassCastException {
        if (value != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(value, locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale));
            jsonObject.addProperty(selectionName, localTime.format(formatter));
        } else {
            jsonObject.addProperty(selectionName, localTime.toString());
        }
    }

    public void format(JsonObject jsonObject, String selectionName, String value, String locale, Number number) throws ClassCastException {
        if (value != null) {
            DecimalFormat decimalFormat = new DecimalFormat(value);
            decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale)));
            jsonObject.addProperty(selectionName, decimalFormat.format(number));
        } else {
            jsonObject.addProperty(selectionName, number);
        }
    }

    public void format(JsonObject jsonObject, String selectionName, String value, String locale, String string) throws ClassCastException {
        if (value != null) {
            Formatter formatter = new Formatter(locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale));
            jsonObject.addProperty(selectionName, formatter.format(value, string).toString());
        } else {
            jsonObject.addProperty(selectionName, string);
        }
    }

    public void format(JsonObject jsonObject, String selectionName, String value, String locale, Boolean bool) throws ClassCastException {
        if (value != null) {
            Formatter formatter = new Formatter(locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale));
            jsonObject.addProperty(selectionName, formatter.format(value, bool).toString());
        } else {
            jsonObject.addProperty(selectionName, bool);
        }
    }

    public void addFormat(JsonArray jsonArray, String value, String locale, LocalDateTime localDateTime) throws ClassCastException {
        if (value != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(value, locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale));
            jsonArray.add(localDateTime.format(formatter));
        } else {
            jsonArray.add(localDateTime.toString());
        }
    }

    public void addFormat(JsonArray jsonArray, String value, String locale, LocalDate localDate) throws ClassCastException {
        if (value != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(value, locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale));
            jsonArray.add(localDate.format(formatter));
        } else {
            jsonArray.add(localDate.toString());
        }
    }

    public void addFormat(JsonArray jsonArray, String value, String locale, LocalTime localTime) throws ClassCastException {
        if (value != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(value, locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale));
            jsonArray.add(localTime.format(formatter));
        } else {
            jsonArray.add(localTime.toString());
        }
    }

    public void addFormat(JsonArray jsonArray, String value, String locale, Number number) throws ClassCastException {
        if (value != null) {
            DecimalFormat decimalFormat = new DecimalFormat(value);
            decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale)));
            jsonArray.add(decimalFormat.format(number));
        } else {
            jsonArray.add(number);
        }
    }

    public void addFormat(JsonArray jsonArray, String value, String locale, String string) throws ClassCastException {
        if (value != null) {
            Formatter formatter = new Formatter(locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale));
            jsonArray.add(formatter.format(value, string).toString());
        } else {
            jsonArray.add(string);
        }
    }

    public void addFormat(JsonArray jsonArray, String value, String locale, Boolean bool) throws ClassCastException {
        if (value != null) {
            Formatter formatter = new Formatter(locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale));
            jsonArray.add(formatter.format(value, bool).toString());
        } else {
            jsonArray.add(bool);
        }
    }
}
