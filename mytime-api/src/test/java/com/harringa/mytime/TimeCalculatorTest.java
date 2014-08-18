package com.harringa.mytime;

import com.harringa.mytime.model.CheckIn;
import com.harringa.mytime.model.HourCalculator;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class TimeCalculatorTest {

    @Test
    public void calculate4HourCheckedInPeriod() throws Exception {
        final CheckIn todayAt9 = new CheckIn(9, 0);
        final CheckIn todayAt1InTheAfternoon = new CheckIn(13, 0);
        assertThat(HourCalculator.hoursBetween(todayAt9, todayAt1InTheAfternoon), is(equalTo(4)));

    }
}
