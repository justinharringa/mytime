package com.harringa.mytime.math;

import java.util.List;

import org.joda.time.*;

public class TimeCalculator {

    public static final PeriodType HOURS_MINUTES_PERIOD = PeriodType.time().withSecondsRemoved().withMillisRemoved();

    public static Period totalTime(final List<Instant> instants) {
        Period totalPeriod = new Period();
        int index = 0;
        while (anotherPairAvailable(instants, index)) {
            totalPeriod = addIntervalOfInstantsToTotal(instants, index, totalPeriod);
            index = indexForNextPair(index);
        }
        final Instant lastInstant = lastInstant(instants);
        if (hasAnInstantWithoutAPair(instants) &&
                (lastInstant.isAfter(beginningOfToday()) ||
                lastInstant.isBeforeNow())) {
            totalPeriod = totalPeriod.plus(millisBetweenNowAndLastInstant(instants));
        }
        return totalPeriod.normalizedStandard(HOURS_MINUTES_PERIOD);
    }

    private static DateTime beginningOfToday() {
        return DateTime.now().dayOfMonth().roundCeilingCopy();
    }

    private static Period addIntervalOfInstantsToTotal(final List<Instant> instants, final int index, final Period totalPeriod) {
        return totalPeriod.plus(periodBetweenPairOfInstantsStartingAt(instants, index));
    }

    private static int indexForNextPair(final int index) {
        return index + 2;
    }

    private static boolean anotherPairAvailable(final List<Instant> instants, final int index) {
        return (index + 1) < instants.size();
    }

    private static Period periodBetweenPairOfInstantsStartingAt(final List<Instant> instants, final int index) {
        // Take a look at using the Interval class...

        return new Interval(roundFloorOfMinute(instants.get(index)),
                roundFloorOfMinute(instants.get(index + 1))).toPeriod(HOURS_MINUTES_PERIOD);
    }

    private static Period millisBetweenNowAndLastInstant(final List<Instant> instants) {
        return new Interval(roundFloorOfMinute(lastInstant(instants)), new Instant())
                .toPeriod(HOURS_MINUTES_PERIOD);
    }

    private static Instant lastInstant(List<Instant> instants) {
        return instants.get(instants.size() - 1);
    }

    public static boolean hasAnInstantWithoutAPair(final List<Instant> instants) {
        return instants.size() % 2 == 1;
    }

    public static Instant roundFloorOfMinute(final Instant instant) {
        return instant.toDateTime().minuteOfDay().roundFloorCopy().toInstant();
    }

}
