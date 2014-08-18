package com.harringa.mytime.model;

import org.joda.time.Hours;

public class HourCalculator {


    public static int hoursBetween(CheckIn start, CheckIn end) {
        return Hours.hoursBetween(start.getDateTime(), end.getDateTime()).getHours();
    }
}
