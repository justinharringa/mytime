package com.harringa.mytime.math;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.google.common.collect.Lists;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class TimeCalculatorTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2024-01-15T12:00:00Z"), ZoneId.systemDefault());
    private static final LocalDateTime NOW = LocalDateTime.now(FIXED_CLOCK);
    public static final Duration FIVE_MINUTES = Duration.ofMinutes(5);

    private static final LocalDateTime FIVE_MINUTES_AGO = NOW.minus(FIVE_MINUTES);
    private static final LocalDateTime THREE_MINUTES_AGO = NOW.minus(Duration.ofMinutes(3));
    private static final LocalDateTime TWO_MINUTES_AGO = NOW.minus(Duration.ofMinutes(2));
    private static final LocalDateTime ONE_MINUTE_AGO = NOW.minus(Duration.ofMinutes(1));
    public static final LocalDateTime FOUR_HOURS_AGO = NOW.minus(Duration.ofHours(4));
    public static final LocalDateTime THREE_HOURS_ONE_MINUTE_AGO = NOW.minus(Duration.ofHours(3))
            .minus(Duration.ofMinutes(1));
    public static final LocalDateTime TWO_HOURS_AGO = NOW.minus(Duration.ofHours(2));
    public static final LocalDateTime ONE_HOUR_ONE_MINUTE_AGO = NOW.minus(Duration.ofHours(1))
            .minus(Duration.ofMinutes(1));

    @BeforeClass
    public static void setUp() throws Exception {
        TimeCalculator.setClock(FIXED_CLOCK);
    }

    @Test
    public void whenTwoInstances5And3MinutesAgoThen2Minutes() throws Exception {
        final List<LocalDateTime> dateTimes = Lists.newArrayList(FIVE_MINUTES_AGO, THREE_MINUTES_AGO);
        assertThat(TimeCalculator.totalTime(dateTimes).toMinutes(),
                is(equalTo(2L)));

    }

    @Test
    public void when5And3And2And1MinutesAgoThen3Minutes() throws Exception {
        final List<LocalDateTime> dateTimes = Lists.newArrayList(FIVE_MINUTES_AGO, THREE_MINUTES_AGO, TWO_MINUTES_AGO, ONE_MINUTE_AGO);
        assertThat(TimeCalculator.totalTime(dateTimes).toMinutes(),
                is(3L));

    }

    @Test
    public void whenOnlyOneInstantFrom5MinutesAgoSentThenYield5Minutes() {
        final List<LocalDateTime> dateTimes = Lists.newArrayList(FIVE_MINUTES_AGO);
        assertThat(TimeCalculator.totalTime(dateTimes).toMinutes(),
                is(equalTo(5L)));

    }

    @Test
    public void whenOnlyOneInstantFor5MinutesFromNowThenYield0Minutes() {
        final List<LocalDateTime> dateTimes = Lists.newArrayList(NOW.plus(FIVE_MINUTES));
        assertThat(TimeCalculator.totalTime(dateTimes).toMinutes(),
                is(equalTo(0L)));
    }

    @Test
    public void whenOnlyOneInstantFrom2DaysAgoThenYield0Minutes() {
        final List<LocalDateTime> dateTimes = Lists.newArrayList(NOW.minus(Duration.ofDays(2)));
        assertThat(TimeCalculator.totalTime(dateTimes).toMinutes(),
                is(equalTo(0L)));
    }

    @Test
    public void testPeriodWith2IntervalsOf59MinutesShouldReturn58Minutes() throws Exception {
        final List<LocalDateTime> dateTimes =
                Lists.newArrayList(FOUR_HOURS_AGO,
                        THREE_HOURS_ONE_MINUTE_AGO,
                        TWO_HOURS_AGO,
                        ONE_HOUR_ONE_MINUTE_AGO);
        assertThat(TimeCalculator.totalTime(dateTimes).toMinutesPart(), is(equalTo(58)));
    }

    @Test
    public void testPeriodWith2IntervalsOf59MinutesShouldReturn1Hour() throws Exception {
        final List<LocalDateTime> dateTimes =
                Lists.newArrayList(FOUR_HOURS_AGO,
                        THREE_HOURS_ONE_MINUTE_AGO,
                        TWO_HOURS_AGO,
                        ONE_HOUR_ONE_MINUTE_AGO);
        assertThat(TimeCalculator.totalTime(dateTimes).toHours(), is(equalTo(1L)));
    }

    @Test
    public void testRoundFloorMillisOfReturnsProperMinute() throws Exception {
        final LocalDateTime almost2Minutes = LocalDateTime.of(2020, 1, 1, 0, 1, 59, 999999999);
        final LocalDateTime oneMinuteMillis = LocalDateTime.of(2020, 1, 1, 0, 1, 0, 0);
        assertThat(TimeCalculator.roundFloorOfMinute(almost2Minutes),
                is(equalTo(oneMinuteMillis)));

    }
}