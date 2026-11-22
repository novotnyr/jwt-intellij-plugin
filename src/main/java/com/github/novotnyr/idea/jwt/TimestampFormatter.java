package com.github.novotnyr.idea.jwt;

import com.github.novotnyr.idea.jwt.Configuration.TimestampFormat;
import com.github.novotnyr.idea.jwt.core.NumericDate;
import org.jetbrains.annotations.Nullable;
import org.ocpsoft.prettytime.PrettyTime;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.Locale;

public final class TimestampFormatter {
    private TimestampFormatter() {
    }

    public static @Nullable String format(@Nullable BigDecimal timestamp, TimestampFormat format, Instant now) {
        if (timestamp == null) {
            return null;
        }
        return switch (format) {
            case RAW -> timestamp.toString();
            case RELATIVE -> formatRelative(timestamp, now);
            case MEDIUM -> formatMedium(timestamp);
            case ISO, ISO_LOCAL -> {
                Instant instant = NumericDate.toInstant(timestamp);
                if (instant == null) {
                    yield null;
                }
                yield format == TimestampFormat.ISO_LOCAL ? formatLocalIso(instant) : instant.toString();
            }
        };
    }

    private static @Nullable String formatMedium(BigDecimal timestamp) {
        Instant instant = NumericDate.toInstant(timestamp);
        if (instant == null) {
            return null;
        }
        try {
            return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                    .withLocale(Locale.getDefault(Locale.Category.FORMAT))
                    .withZone(ZoneId.systemDefault())
                    .format(instant);
        } catch (DateTimeException e) {
            return null;
        }
    }

    public static @Nullable String formatLocalIso(Instant instant) {
        try {
            ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(instant);
            if (ZoneOffset.UTC.equals(offset)) {
                return instant.toString();
            }
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(instant.atOffset(offset));
        } catch (DateTimeException e) {
            return null;
        }
    }

    private static @Nullable String formatRelative(BigDecimal timestamp, Instant now) {
        Date date = NumericDate.toDate(timestamp);
        Date reference = NumericDate.toDate(NumericDate.fromInstant(now));
        if (date == null || reference == null) {
            return null;
        }
        long difference;
        try {
            difference = Math.subtractExact(date.getTime(), reference.getTime());
        } catch (ArithmeticException e) {
            return null;
        }
        if (difference == Long.MIN_VALUE) {
            return null;
        }
        return difference == 0 ? "just now" : new PrettyTime(reference).format(date);
    }
}