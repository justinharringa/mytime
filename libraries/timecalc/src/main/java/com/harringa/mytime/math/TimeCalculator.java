package com.harringa.mytime.math;

import org.joda.time.Instant;

public class TimeCalculator {

    public static long totalTime(final Instant... instants) {
        long totalTime = 0L;
        for (int i = 0; i < numberOfPairsIn(instants); i++) {
            totalTime += millisBetweenPairOfInstantsStartingAt(instants, i);
        }
        if (hasAnInstantWithoutAPair(instants)) {
            totalTime += millisBetweenNowAndLastInstant(instants);
        }
        return totalTime;
    }

    private static int numberOfPairsIn(final Instant[] instants) {
        return instants.length / 2;
    }

    private static long millisBetweenPairOfInstantsStartingAt(final Instant[] instants, final int index) {
        return instants[index].getMillis() - instants[index + 1].getMillis();
    }

    private static long millisBetweenNowAndLastInstant(final Instant[] instants) {
        return new Instant().getMillis() - instants[instants.length - 1].getMillis();
    }

    private static boolean hasAnInstantWithoutAPair(final Instant[] instants) {
        return instants.length % 2 == 1;
    }

}
