package com.harringa.mytime;

import static com.harringa.mytime.testing.Waits.waitUntil;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TimePicker;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.harringa.mytime.repository.CheckInContentProvider;
import com.harringa.mytime.testing.ResetCheckInContentProviderRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowToast;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public class MyTimeMainActivityTest {

    private static final LocalDateTime EARLIER_CHECK_IN = LocalDateTime.of(2026, 1, 5, 9, 0);

    @Rule
    public final ResetCheckInContentProviderRule resetProvider = new ResetCheckInContentProviderRule();

    private CheckInContentProvider provider;

    @Before
    public void saveAnEarlierCheckIn() {
        provider = CheckInContentProvider.getInstance(ApplicationProvider.getApplicationContext());
        assertTrue(provider.saveCheckIn(EARLIER_CHECK_IN));
    }

    @Test
    public void showsSavedCheckInsOnLaunch() {
        try (ActivityScenario<MyTimeMainActivity> scenario = ActivityScenario.launch(MyTimeMainActivity.class)) {
            waitForListRows(scenario, 1);
        }
    }

    // Regression: onDestroy() closed the shared CheckInContentProvider, so the recreated
    // activity (rotation, any configuration change) crashed loading the list
    @Test
    public void listLoadsAgainAfterTheActivityIsRecreated() {
        try (ActivityScenario<MyTimeMainActivity> scenario = ActivityScenario.launch(MyTimeMainActivity.class)) {
            waitForListRows(scenario, 1);

            scenario.recreate();

            waitForListRows(scenario, 1);
        }
    }

    // Regression: same bug when the app is reopened after Back while the process is still alive
    @Test
    public void listLoadsWhenReopenedInTheSameProcess() {
        try (ActivityScenario<MyTimeMainActivity> first = ActivityScenario.launch(MyTimeMainActivity.class)) {
            waitForListRows(first, 1);
        }

        try (ActivityScenario<MyTimeMainActivity> reopened = ActivityScenario.launch(MyTimeMainActivity.class)) {
            waitForListRows(reopened, 1);
        }
    }

    @Test
    public void checkInButtonSavesTheSelectedTimeForToday() {
        try (ActivityScenario<MyTimeMainActivity> scenario = ActivityScenario.launch(MyTimeMainActivity.class)) {
            waitForListRows(scenario, 1);
            final LocalDate dayBefore = LocalDate.now();

            checkInAt(scenario, 7, 30);
            waitForListRows(scenario, 2);

            // Allow for the test running across midnight
            final LocalDateTime saved = provider.getAll().get(0);
            assertEquals(7, saved.getHour());
            assertEquals(30, saved.getMinute());
            assertTrue(saved.toLocalDate().equals(dayBefore) || saved.toLocalDate().equals(LocalDate.now()));
        }
    }

    @Test
    public void duplicateCheckInIsSkippedWithAMessage() {
        try (ActivityScenario<MyTimeMainActivity> scenario = ActivityScenario.launch(MyTimeMainActivity.class)) {
            waitForListRows(scenario, 1);

            checkInAt(scenario, 7, 30);
            assertNull(ShadowToast.getTextOfLatestToast());
            checkInAt(scenario, 7, 30);

            assertEquals("Already checked in at 07:30", ShadowToast.getTextOfLatestToast());
            final List<LocalDateTime> checkIns = provider.getAll();
            assertEquals(2, checkIns.size());
            assertEquals(EARLIER_CHECK_IN, checkIns.get(1));
        }
    }

    /** Picks a time, taps Check In, and waits for the save to finish (the button re-enables). */
    private static void checkInAt(ActivityScenario<MyTimeMainActivity> scenario, int hour, int minute) {
        scenario.onActivity(activity -> {
            TimePicker timePicker = activity.findViewById(R.id.timePicker);
            timePicker.setHour(hour);
            timePicker.setMinute(minute);
            activity.findViewById(R.id.checkIn).performClick();
        });
        waitUntil("the check-in to be saved", () -> {
            AtomicBoolean enabled = new AtomicBoolean();
            scenario.onActivity(activity -> enabled.set(activity.findViewById(R.id.checkIn).isEnabled()));
            return enabled.get();
        });
    }

    private static void waitForListRows(ActivityScenario<MyTimeMainActivity> scenario, int rows) {
        waitUntil("the list to show " + rows + " row(s)", () -> listRowCount(scenario) == rows);
    }

    /** Rows in the check-in list, or -1 before the list has loaded. */
    private static int listRowCount(ActivityScenario<MyTimeMainActivity> scenario) {
        AtomicInteger count = new AtomicInteger();
        scenario.onActivity(activity -> {
            ListAdapter adapter = ((ListView) activity.findViewById(R.id.checkInListView)).getAdapter();
            count.set(adapter == null ? -1 : adapter.getCount());
        });
        return count.get();
    }
}
