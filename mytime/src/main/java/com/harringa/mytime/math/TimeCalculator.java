package com.harringa.mytime.math;

import org.joda.time.DateTimeConstants;
import org.joda.time.Instant;

public class TimeCalculator {

    public static long totalTime(final Instant... instants) {
        final int numLeftoverInstants = instants.length % 2;
        final int numPairs = instants.length / 2;
        long totalTime = 0L;
        for (int i = 0; i < numPairs; i++) {
            totalTime += instants[i + 1].getMillis() - instants[i].getMillis();
        }
        if (numLeftoverInstants == 1) {

            final long timeBetweenLastInstanceAndNow =
                    new Instant().getMillis() - instants[instants.length - 1].getMillis();
            if (timeBetweenLastInstanceAndNow < DateTimeConstants.MILLIS_PER_DAY) {
                totalTime += timeBetweenLastInstanceAndNow;
            }
        }
        return totalTime;
    }

}
