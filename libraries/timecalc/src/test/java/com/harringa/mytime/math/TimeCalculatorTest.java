package com.harringa.mytime.math;

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
    private static final Instant TWO_MINUTES_AGO = NOW.minus(Minutes.TWO.toStandardDuration());
    private static final Instant ONE_MINUTE_AGO = NOW.minus(Minutes.ONE.toStandardDuration());

    @BeforeClass
    public static void setUp() throws Exception {
        DateTimeUtils.setCurrentMillisFixed(NOW.getMillis());
    }

    @Test
    public void whenTwoInstances5And3MinutesAgoThen2Minutes() throws Exception {
        final List<Instant> instants = Lists.newArrayList(FIVE_MINUTES_AGO, THREE_MINUTES_AGO);
        assertThat(TimeCalculator.totalTime(instants).getMinutes(),
                is(equalTo(2)));

    }

    @Test
    public void when5And3And2And1MinutesAgoThen3Minutes() throws Exception {
        final List<Instant> instants = Lists.newArrayList(FIVE_MINUTES_AGO, THREE_MINUTES_AGO, TWO_MINUTES_AGO, ONE_MINUTE_AGO);
        assertThat(TimeCalculator.totalTime(instants).getMinutes(),
                is(3));

    }

    @Test
    public void whenOnlyOneInstantFrom5MinutesAgoSentThenYield5Minutes() {
        final List<Instant> instants = Lists.newArrayList(FIVE_MINUTES_AGO);
        assertThat(TimeCalculator.totalTime(instants).getMinutes(),
                is(equalTo(5)));

    }

    @Test
    public void testRoundFloorMillisOfReturnsProperMinute() throws Exception {
        final Instant almost2Minutes = new Instant(0).plus(Minutes.TWO.toStandardDuration()).minus(1);
        final Instant oneMinuteMillis = new Instant(0).plus(Minutes.ONE.toStandardDuration());
        assertThat(TimeCalculator.roundFloorOfMinute(almost2Minutes),
                is(equalTo(oneMinuteMillis)));

    }
}