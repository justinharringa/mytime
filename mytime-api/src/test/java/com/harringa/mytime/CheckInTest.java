package com.harringa.mytime;

import com.harringa.mytime.model.CheckIn;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CheckInTest {

    @Test
    public void testCheckInWithProvidedDateReturnsSameDate() throws Exception {
        final Date now = new Date();
        final CheckIn checkIn = new CheckIn(now);
        assertThat(checkIn.getDate(), is(now));
    }
}
