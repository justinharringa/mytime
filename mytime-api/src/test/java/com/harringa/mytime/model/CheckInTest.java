package com.harringa.mytime.model;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class CheckInTest {

    @Test
    public void testCheckInWithProvidedMinuteAndHourReturnsSameDateWithPrecisionToMinutes() throws Exception {
        final CheckIn now = new CheckIn(new DateTime());
        final CheckIn checkIn = new CheckIn(now.getHourOfDay(), now.getMinuteOfHour());
        assertThat(checkIn, is(equalTo(now)));

    }

    @Test
    public void testCheckInWithProvidedDateMinuteAndHourReturnsSameDateWithPrecisionToMinutes() throws Exception {
        final CheckIn now = new CheckIn(new DateTime());
        final CheckIn checkIn =
                new CheckIn(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), now.getHourOfDay(), now.getMinuteOfHour());
        assertThat(checkIn, is(equalTo(now)));
    }

}
