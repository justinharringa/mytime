package com.harringa.mytime.testing;

import com.harringa.mytime.repository.CheckInContentProvider;

import org.junit.rules.ExternalResource;

import java.lang.reflect.Field;

/**
 * Clears the process-wide {@link CheckInContentProvider} singleton around each test.
 *
 * <p>Robolectric gives every test a fresh application and database, but static fields survive
 * between tests, so without this a test would get a provider bound to the previous test's
 * (deleted) database.
 */
public class ResetCheckInContentProviderRule extends ExternalResource {

    @Override
    protected void before() {
        reset();
    }

    @Override
    protected void after() {
        reset();
    }

    /** Drops the singleton, as if the app process had been restarted. */
    public static void reset() {
        try {
            Field instance = CheckInContentProvider.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("CheckInContentProvider.instance not found; update this rule", e);
        }
    }
}
