package com.dnyferguson.thepunisher.utils;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

public class Time {
    public static Timestamp getForwards(String input) {
        int minutes = inputToMinutes(input);

        Timestamp timestamp = new Timestamp(new Date().getTime());

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp.getTime());

        // add minutes
        cal.add(Calendar.MINUTE, minutes);
        return new Timestamp(cal.getTime().getTime());
    }

    public static Timestamp getBackwards(String input) {
        int minutes = inputToMinutes(input);

        Timestamp timestamp = new Timestamp(new Date().getTime());
        System.out.println(timestamp);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp.getTime());

        // remove minutes
        cal.add(Calendar.MINUTE, -minutes);
        return new Timestamp(cal.getTime().getTime());
    }

    private static int inputToMinutes(String input) {
        int minutes = 0;

        if (input.endsWith("d")) {
            minutes += Integer.valueOf(input.split("d")[0]) * 1440;
        }
        if (input.endsWith("h")) {
            minutes += Integer.valueOf(input.split("h")[0]) * 60;
        }
        if (input.endsWith("m")) {
            minutes += Integer.valueOf(input.split("m")[0]);
        }

        return minutes;
    }
}
