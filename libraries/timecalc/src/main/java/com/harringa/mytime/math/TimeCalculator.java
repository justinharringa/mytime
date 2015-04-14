package com.harringa.mytime.math;

import java.util.List;

import org.joda.time.Instant;

public class TimeCalculator {

    public static long totalTime(final List<Instant> instants) {
        long totalTime = 0L;
        for (int i = 0; i < numberOfPairsIn(instants); i++) {
            totalTime += millisBetweenPairOfInstantsStartingAt(instants, i);
        }
        if (hasAnInstantWithoutAPair(instants)) {
            totalTime += millisBetweenNowAndLastInstant(instants);
        }
        return totalTime;
    }

    private static int numberOfPairsIn(final List<Instant> instants) {
        return instants.size() / 2;
    }

    private static long millisBetweenPairOfInstantsStartingAt(final List<Instant> instants, final int index) {
        // Take a look at using the Interval class...
        return instants.get(index + 1).getMillis() - instants.get(index).getMillis();
    }

    private static long millisBetweenNowAndLastInstant(final List<Instant> instants) {
        return new Instant().getMillis() - instants.get(instants.size() - 1).getMillis();
    }

    private static boolean hasAnInstantWithoutAPair(final List<Instant> instants) {
        return instants.size() % 2 == 1;
    }

}
