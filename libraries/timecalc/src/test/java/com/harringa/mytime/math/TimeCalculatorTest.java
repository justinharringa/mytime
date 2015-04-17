package com.harringa.mytime.math;

import java.util.List;

import com.google.common.collect.Lists;
import org.joda.time.*;
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
    public static final Instant FOUR_HOURS_AGO = NOW.minus(Hours.hours(4).toStandardDuration());
    public static final Instant THREE_HOURS_ONE_MINUTE_AGO = NOW.minus(Hours.hours(3).toStandardDuration())
            .minus(Minutes.ONE.toStandardDuration());
    public static final Instant TWO_HOURS_AGO = NOW.minus(Hours.hours(2).toStandardDuration());
    public static final Instant ONE_HOUR_ONE_MINUTE_AGO = NOW.minus(Hours.hours(1).toStandardDuration())
            .minus(Minutes.ONE.toStandardDuration());

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
    public void testPeriodWith2IntervalsOf59MinutesShouldReturn58Minutes() throws Exception {
        final List<Instant> instants =
                Lists.newArrayList(FOUR_HOURS_AGO,
                        THREE_HOURS_ONE_MINUTE_AGO,
                        TWO_HOURS_AGO,
                        ONE_HOUR_ONE_MINUTE_AGO);
        assertThat(TimeCalculator.totalTime(instants).getMinutes(),
                is(equalTo(Minutes.minutes(58).getMinutes())));

    }

    @Test
    public void testPeriodWith2IntervalsOf59MinutesShouldReturn1Hour() throws Exception {
        final List<Instant> instants =
                Lists.newArrayList(FOUR_HOURS_AGO,
                        THREE_HOURS_ONE_MINUTE_AGO,
                        TWO_HOURS_AGO,
                        ONE_HOUR_ONE_MINUTE_AGO);
        assertThat(TimeCalculator.totalTime(instants).getHours(),
                is(equalTo(Hours.ONE.getHours())));

    }

    @Test
    public void testRoundFloorMillisOfReturnsProperMinute() throws Exception {
        final Instant almost2Minutes = new Instant(0).plus(Minutes.TWO.toStandardDuration()).minus(1);
        final Instant oneMinuteMillis = new Instant(0).plus(Minutes.ONE.toStandardDuration());
        assertThat(TimeCalculator.roundFloorOfMinute(almost2Minutes),
                is(equalTo(oneMinuteMillis)));

    }
}