package com.harringa.mytime.math;

import java.util.List;

import org.joda.time.Instant;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.PeriodType;

public class TimeCalculator {

    public static final PeriodType HOURS_MINUTES_PERIOD = PeriodType.time().withSecondsRemoved().withMillisRemoved();

    public static Period totalTime(final List<Instant> instants) {
        Period totalPeriod = new Period();
        int index = 0;
        while (anotherPairAvailable(instants, index)) {
            totalPeriod = totalPeriod.plus(periodBetweenPairOfInstantsStartingAt(instants, index));
            index = indexForNextPair(index);
        }
        if (hasAnInstantWithoutAPair(instants)) {
            totalPeriod = totalPeriod.plus(millisBetweenNowAndLastInstant(instants));
        }
        return totalPeriod;
    }

    private static int indexForNextPair(final int index) {
        return index + 2;
    }

    private static boolean anotherPairAvailable(final List<Instant> instants, final int index) {
        return (index + 1) < instants.size();
    }

    private static int numberOfPairsIn(final List<Instant> instants) {
        return instants.size() / 2;
    }

    private static Period periodBetweenPairOfInstantsStartingAt(final List<Instant> instants, final int index) {
        // Take a look at using the Interval class...

        return new Interval(roundFloorOfMinute(instants.get(index)),
                roundFloorOfMinute(instants.get(index + 1))).toPeriod(HOURS_MINUTES_PERIOD);
    }

    private static Period millisBetweenNowAndLastInstant(final List<Instant> instants) {
        return new Interval(roundFloorOfMinute(instants.get(instants.size() - 1)), new Instant())
                .toPeriod(HOURS_MINUTES_PERIOD);
    }

    public static boolean hasAnInstantWithoutAPair(final List<Instant> instants) {
        return instants.size() % 2 == 1;
    }

    public static Instant roundFloorOfMinute(final Instant instant) {
        return instant.toDateTime().minuteOfDay().roundFloorCopy().toInstant();
    }

}
