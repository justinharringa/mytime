package com.harringa.mytime.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.harringa.mytime.testing.ResetCheckInContentProviderRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class CheckInContentProviderTest {

    private static final LocalDateTime NINE_AM = LocalDateTime.of(2026, 1, 5, 9, 0);

    @Rule
    public final ResetCheckInContentProviderRule resetProvider = new ResetCheckInContentProviderRule();

    private final Context context = ApplicationProvider.getApplicationContext();
    private CheckInContentProvider provider;

    @Before
    public void setUp() {
        provider = CheckInContentProvider.getInstance(context);
    }

    @Test
    public void getAllIsEmptyBeforeAnyCheckIn() {
        assertEquals(Collections.emptyList(), provider.getAll());
    }

    @Test
    public void getAllReturnsCheckInsNewestFirst() {
        assertTrue(provider.saveCheckIn(NINE_AM));
        assertTrue(provider.saveCheckIn(NINE_AM.withHour(17)));
        assertTrue(provider.saveCheckIn(NINE_AM.withHour(12)));

        assertEquals(
                Arrays.asList(NINE_AM.withHour(17), NINE_AM.withHour(12), NINE_AM),
                provider.getAll());
    }

    @Test
    public void checkInAtTheSameTimeIsSkippedAsDuplicate() {
        assertTrue(provider.saveCheckIn(NINE_AM));

        assertFalse(provider.saveCheckIn(NINE_AM));
        assertEquals(Collections.singletonList(NINE_AM), provider.getAll());
    }

    @Test
    public void checkInTwoMinutesLaterIsNotADuplicate() {
        assertTrue(provider.saveCheckIn(NINE_AM));

        assertTrue(provider.saveCheckIn(NINE_AM.plusMinutes(2)));
        assertEquals(Arrays.asList(NINE_AM.plusMinutes(2), NINE_AM), provider.getAll());
    }

    @Test
    public void getInstanceReturnsTheSameProvider() {
        assertSame(provider, CheckInContentProvider.getInstance(context));
    }

    @Test
    public void checkInsPersistAcrossProviderInstances() {
        assertTrue(provider.saveCheckIn(NINE_AM));

        ResetCheckInContentProviderRule.reset();
        CheckInContentProvider reopened = CheckInContentProvider.getInstance(context);

        assertEquals(Collections.singletonList(NINE_AM), reopened.getAll());
    }
}
