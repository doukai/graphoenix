package io.graphoenix.core.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

public enum CodecUtil {

    CODEC_UTIL;

    public String encode(LocalDateTime localDateTime) throws ClassCastException {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.format(ISO_LOCAL_DATE_TIME);
    }

    public String encode(LocalDate localDate) throws ClassCastException {
        if (localDate == null) {
            return null;
        }
        return localDate.format(ISO_LOCAL_DATE);
    }

    public String encode(LocalTime localTime) throws ClassCastException {
        if (localTime == null) {
            return null;
        }
        return localTime.format(ISO_LOCAL_TIME);
    }

    public LocalDateTime decodeLocalDateTime(String value) throws ClassCastException {
        if (value == null) {
            return null;
        }
        return LocalDateTime.parse(value, ISO_LOCAL_DATE_TIME);
    }

    public LocalDate decodeLocalDate(String value) throws ClassCastException {
        if (value == null) {
            return null;
        }
        return LocalDate.parse(value, ISO_LOCAL_DATE);
    }

    public LocalTime decodeLocalTime(String value) throws ClassCastException {
        if (value == null) {
            return null;
        }
        return LocalTime.parse(value, ISO_LOCAL_TIME);
    }
}
