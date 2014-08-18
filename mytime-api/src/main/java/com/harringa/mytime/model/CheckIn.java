package com.harringa.mytime.model;

import org.joda.time.DateTime;

public class CheckIn {

    private final DateTime dateTime;

    public CheckIn(final DateTime dateTime) {
        this.dateTime = dateTime;
    }

    public CheckIn(int hourOfDay, int minuteOfDay) {
        this(new DateTime().withTimeAtStartOfDay()
                .withHourOfDay(hourOfDay)
                .withMinuteOfHour(minuteOfDay));
    }

    public CheckIn(int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour) {
        this(new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour));
    }

    DateTime getDateTime() {
        return dateTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CheckIn checkIn = (CheckIn) o;

        if (dateTime != null ? !dateAndTimeWithPrecisionOfMinutesEquals(checkIn) : checkIn.dateTime != null) return false;

        return true;
    }

    private boolean dateAndTimeWithPrecisionOfMinutesEquals(CheckIn checkIn) {
        if (getYear() != checkIn.getYear()) return false;
        if (getMonthOfYear() != checkIn.getMonthOfYear()) return false;
        if (getDayOfMonth() != checkIn.getDayOfMonth()) return false;
        if (getHourOfDay() != checkIn.getHourOfDay()) return false;
        if (getMinuteOfHour() != checkIn.getMinuteOfHour()) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return dateTime != null ? dateTime.hashCode() : 0;
    }

    public int getYear() {
        return dateTime.getYear();
    }

    public int getMonthOfYear() {
        return dateTime.getMonthOfYear();
    }

    public int getDayOfMonth() {
        return dateTime.getDayOfMonth();
    }

    public int getHourOfDay() {
        return dateTime.getHourOfDay();
    }

    public int getMinuteOfHour() {
        return dateTime.getMinuteOfHour();
    }
}
