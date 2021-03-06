package com.github.novotnyr.idea.jwt;

import java.util.Date;

public class DateUtils {
    public static Date toDate(String unixTimestamp) {
        try {
            long longUnixTimestamp = Long.parseLong(unixTimestamp);
            return new Date(longUnixTimestamp * 1000);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Illegal format of unix timestamp: " + unixTimestamp, e);
        }
    }

    public static Date toDate(long unixTimestamp) {
        return new Date(unixTimestamp * 100);
    }

    public static String toTimestampString(Date date) {
        long unixTimestamp = date.getTime() / 1000;
        return String.valueOf(unixTimestamp);
    }

    public static String addSeconds(String unixTimestampString, int seconds, String errorValue) {
        try {
            long unixTimestamp = Long.parseLong(unixTimestampString);
            unixTimestamp = unixTimestamp + seconds;
            return String.valueOf(unixTimestamp);
        } catch (NumberFormatException e) {
            return errorValue;
        }
    }
}
