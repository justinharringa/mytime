package com.harringa.mytime.math;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class TimeCalculator {

    private static Clock clock = Clock.systemDefaultZone();

    public static void setClock(Clock clock) {
        TimeCalculator.clock = clock;
    }

    public static Duration totalTime(final List<LocalDateTime> dateTimes) {
        Duration totalDuration = Duration.ZERO;
        int index = 0;
        while (anotherPairAvailable(dateTimes, index)) {
            totalDuration = addIntervalOfDateTimesToTotal(dateTimes, index, totalDuration);
            index = indexForNextPair(index);
        }
        final LocalDateTime lastDateTime = lastDateTime(dateTimes);
        LocalDateTime now = LocalDateTime.now(clock);
        if (hasAnInstantWithoutAPair(dateTimes) &&
                (lastDateTime.isAfter(beginningOfToday()) ||
                lastDateTime.isBefore(now))) {
            totalDuration = totalDuration.plus(millisBetweenNowAndLastDateTime(dateTimes));
        }
        return totalDuration;
    }

    private static LocalDateTime beginningOfToday() {
        return LocalDateTime.now(clock).toLocalDate().atStartOfDay().plusDays(1);
    }

    private static Duration addIntervalOfDateTimesToTotal(final List<LocalDateTime> dateTimes, final int index, final Duration totalDuration) {
        return totalDuration.plus(durationBetweenPairOfDateTimesStartingAt(dateTimes, index));
    }

    private static int indexForNextPair(final int index) {
        return index + 2;
    }

    private static boolean anotherPairAvailable(final List<LocalDateTime> dateTimes, final int index) {
        return (index + 1) < dateTimes.size();
    }

    private static Duration durationBetweenPairOfDateTimesStartingAt(final List<LocalDateTime> dateTimes, final int index) {
        return Duration.between(
            roundFloorOfMinute(dateTimes.get(index)),
            roundFloorOfMinute(dateTimes.get(index + 1))
        );
    }

    private static Duration millisBetweenNowAndLastDateTime(final List<LocalDateTime> dateTimes) {
        return Duration.between(
            roundFloorOfMinute(lastDateTime(dateTimes)), 
            LocalDateTime.now(clock)
        );
    }

    private static LocalDateTime lastDateTime(List<LocalDateTime> dateTimes) {
        return dateTimes.get(dateTimes.size() - 1);
    }

    public static boolean hasAnInstantWithoutAPair(final List<LocalDateTime> dateTimes) {
        return dateTimes.size() % 2 == 1;
    }

    public static LocalDateTime roundFloorOfMinute(final LocalDateTime dateTime) {
        return dateTime.truncatedTo(ChronoUnit.MINUTES);
    }

}
