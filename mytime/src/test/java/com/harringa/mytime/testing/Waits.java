package com.harringa.mytime.testing;

import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;

import java.time.Duration;
import java.util.function.BooleanSupplier;

public final class Waits {

    private static final long TIMEOUT_MS = 5_000;

    private Waits() {
    }

    /**
     * Waits for work that hops between the main thread and the activity's background database
     * thread. Each round runs everything due on the (paused) Robolectric main looper, advancing
     * its clock past the activity's 100 ms debounce, then gives the database thread a moment to
     * post results back.
     */
    public static void waitUntil(String description, BooleanSupplier condition) {
        final long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (true) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));
            if (condition.getAsBoolean()) {
                return;
            }
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("Timed out waiting for " + description);
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }
    }
}
