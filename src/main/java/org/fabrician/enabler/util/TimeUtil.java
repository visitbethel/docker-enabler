/*
 * Copyright (c) 2014 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
package org.fabrician.enabler.util;

import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.google.common.math.LongMath;

/**
 * An utility class to handle date time in RFC3339 format. Example : "2014-10-22T20:40:59.177118849Z". This can be parsed to obtained the millisecs since "2006-01-02T15:04:05.999999999Z07:00" epoch.
 * <code>
 * long start_time=TimeUtil.durationInMillisecs("2014-10-22T20:40:59.177118849Z");
 * </code>
 * 
 */
public class TimeUtil {
    // durations in millisecs
    private static final long SEC_DURATION = 1000;
    private static final long MIN_DURATION = 60 * SEC_DURATION;
    private static final long HR_DURATION = 60 * MIN_DURATION;
    private static final long DAY_DURATION = 24 * HR_DURATION;
    private static final long WEEK_DURATION = 7 * DAY_DURATION;

    private TimeUtil() {}

    /**
     * Get date time string in millisecs since the RFC3339 epoch
     * 
     * @param rfc3339_datetime
     *            the date time string in RFC3339
     * @return date time in milliseconds
     */
    public static long toMillisecs(String rfc3339_datetime) {
        DateTime dateTime = new DateTime(rfc3339_datetime, DateTimeZone.UTC);
        return dateTime.getMillis();
    }

    /**
     * Get the duration since the RFC3339 date time string
     * 
     * @param rfc3339_datetime
     *            the date time string in RFC3339
     * @return the duration in millisecs
     */
    public static long durationInMillisecs(String rfc3339_datetime) {
        return (System.currentTimeMillis() - toMillisecs(rfc3339_datetime));
    }

    /**
     * Format duration message in integral units of increasing coarse grains for display purpose
     * 
     * @param duration
     *            the duration in millisecs
     * @return a formatted duration string in secs, mins,hrs or weeks
     */
    public static String formatDuration(long duration) {
        Validate.isTrue(duration >= 0, "Duration is negative", duration);
        if (duration < SEC_DURATION) {
            return duration + "ms";
        } else if (duration >= SEC_DURATION && duration < MIN_DURATION) {
            return formatDurationMsg(TimeUnit.MILLISECONDS.toSeconds(duration), "sec");
        } else if (duration >= MIN_DURATION && duration < HR_DURATION) {
            long num_secs=TimeUnit.MILLISECONDS.toSeconds(duration);
            long num_mins=LongMath.divide(num_secs,60,RoundingMode.DOWN);
            long residue_secs=num_secs-(num_mins * 60);
            return formatDurationMsg(num_mins, "min", residue_secs, "sec");
        } else if (duration >= HR_DURATION && duration < DAY_DURATION) {
            long num_mins=TimeUnit.MILLISECONDS.toMinutes(duration);
            long num_hrs=LongMath.divide(num_mins,60,RoundingMode.DOWN);
            long residue_mins=num_mins-(num_hrs * 60);
            return formatDurationMsg(num_hrs, "hr",residue_mins,"min");
        } else if (duration >= DAY_DURATION && duration < WEEK_DURATION) {
            long num_hrs=TimeUnit.MILLISECONDS.toHours(duration);
            long num_days=LongMath.divide(num_hrs,24,RoundingMode.DOWN);
            long residue_hrs=num_hrs-(num_days * 24);
            return formatDurationMsg(num_days, "day",residue_hrs,"hr");
        } else {
            long num_days=TimeUnit.MILLISECONDS.toDays(duration);
            long num_weeks=LongMath.divide(num_days,7,RoundingMode.DOWN);
            long residue_days=num_days-(num_weeks * 7);
            return formatDurationMsg(num_weeks,"week", residue_days,"day");
        }

    }

    /**
     * Format coarse-grain duration message since a "start time" in RFC3339
     * 
     * @param rfc3339_datetime
     * @return a formatted duration string in secs, mins,hrs or weeks
     */
    public static String formatDurationMsg(String rfc3339_datetime) {
        long duration = durationInMillisecs(rfc3339_datetime);
        return formatDuration(duration);
    }

    private static String formatDurationMsg(long majorDuration, String majorUnit, long minorDuration, String minorUnit) {
        String primaryMsg = formatDurationMsg(majorDuration,majorUnit);
        String secondaryMsg = formatDurationMsg(minorDuration,minorUnit);
        return primaryMsg + " " + secondaryMsg;
    }

    private static String formatDurationMsg(long duration, String unit) {
        String msg = duration + " " + unit;
        return (duration == 1) ? msg : msg + "s";
    }
}
