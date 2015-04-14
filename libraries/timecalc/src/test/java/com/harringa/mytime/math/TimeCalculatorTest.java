package com.harringa.mytime.math;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
import org.joda.time.DateTimeUtils;
import org.joda.time.Instant;
import org.joda.time.Minutes;
import org.joda.time.ReadableDuration;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class TimeCalculatorTest {

    private static final Instant NOW = Instant.now();
    public static final ReadableDuration FIVE_MINUTES = Minutes.minutes(5).toStandardDuration();

    private static final Instant FIVE_MINUTES_AGO = NOW.minus(FIVE_MINUTES);
    private static final Instant THREE_MINUTES_AGO = NOW.minus(Minutes.THREE.toStandardDuration());

    @BeforeClass
    public static void setUp() throws Exception {
        DateTimeUtils.setCurrentMillisFixed(NOW.getMillis());
    }

    @Test
    public void whenTwoInstances5And7MinutesAgoThen2Minutes() throws Exception {
        final List<Instant> instants = Lists.newArrayList(FIVE_MINUTES_AGO, THREE_MINUTES_AGO);
        assertThat(TimeCalculator.totalTime(instants),
                is(equalTo(Minutes.TWO.toStandardDuration().getMillis())));

    }

    @Test
    public void whenOnlyOneInstantFrom5MinutesAgoSentThenYield5Minutes() {
        final List<Instant> instants = Lists.newArrayList(FIVE_MINUTES_AGO);
        assertThat(TimeCalculator.totalTime(instants),
                is(equalTo(FIVE_MINUTES.getMillis())));

    }
}