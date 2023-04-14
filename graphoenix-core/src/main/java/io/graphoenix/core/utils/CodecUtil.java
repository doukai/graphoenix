package io.graphoenix.core.utils;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import com.google.type.Date;
import com.google.type.Decimal;
import com.google.type.TimeOfDay;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

public enum CodecUtil {

    CODEC_UTIL;

    public Timestamp encode(LocalDateTime localDateTime) throws ClassCastException {
        if (localDateTime == null) {
            return null;
        }
        Instant instant = localDateTime.toInstant(ZoneOffset.UTC);
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    public Date encode(LocalDate localDate) throws ClassCastException {
        if (localDate == null) {
            return null;
        }
        return Date.newBuilder()
                .setYear(localDate.getYear())
                .setMonth(localDate.getMonthValue())
                .setDay(localDate.getDayOfMonth())
                .build();
    }

    public TimeOfDay encode(LocalTime localTime) throws ClassCastException {
        if (localTime == null) {
            return null;
        }
        return TimeOfDay.newBuilder()
                .setHours(localTime.getHour())
                .setMinutes(localTime.getMinute())
                .setSeconds(localTime.getSecond())
                .setNanos(localTime.getNano())
                .build();
    }

    public Decimal encode(BigDecimal bigDecimal) throws ClassCastException {
        if (bigDecimal == null) {
            return null;
        }
        return Decimal.newBuilder()
                .setValueBytes(ByteString.copyFrom(bigDecimal.unscaledValue().toByteArray()))
                .build();
    }

    public Decimal encode(BigInteger bigInteger) throws ClassCastException {
        if (bigInteger == null) {
            return null;
        }
        return Decimal.newBuilder()
                .setValueBytes(ByteString.copyFrom(bigInteger.toByteArray()))
                .build();
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
