package com.harringa.mytime.math;

import org.joda.time.*;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class TimeCalculatorTest {

    private static final Instant NOW = Instant.now();
    public static final ReadableDuration FIVE_MINUTES = Minutes.minutes(5).toStandardDuration();

    private static final Instant FIVE_MINUTES_AGO = NOW.minus(FIVE_MINUTES);
    private static final Instant SEVEN_MINUTES_AGO = NOW.minus(Minutes.THREE.toStandardDuration());

    @BeforeClass
    public static void setUp() throws Exception {
        DateTimeUtils.setCurrentMillisFixed(NOW.getMillis());
    }

    @Test
    public void whenTwoInstances5And7MinutesAgoThen2Minutes() throws Exception {
        assertThat(TimeCalculator.totalTime(SEVEN_MINUTES_AGO, FIVE_MINUTES_AGO),
                is(equalTo(Minutes.TWO.toStandardDuration().getMillis())));

    }

    @Test
    public void whenOnlyOneInstantFrom5MinutesAgoSentThenYield5Minutes() {
        assertThat(TimeCalculator.totalTime(FIVE_MINUTES_AGO),
                is(equalTo(FIVE_MINUTES.getMillis())));

    }
}