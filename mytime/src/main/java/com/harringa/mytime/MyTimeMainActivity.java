package com.harringa.mytime;

import android.app.Activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import com.harringa.mytime.repository.CheckInContentProvider;
import com.harringa.mytime.view.CheckInAdapter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyTimeMainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "MyTimeMainActivity";
    // One background thread for the whole process, so database work runs in order even
    // across activity recreation (e.g. a save started just before rotation finishes before
    // the new activity reloads the list)
    private static final ExecutorService DATABASE_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final DateTimeFormatter GROUP_BY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private CheckInContentProvider checkInContentProvider;
    private ListView checkInList;
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private final Runnable debouncedUpdate = new Runnable() {
        @Override
        public void run() {
            performUpdateCheckInList();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkInContentProvider = CheckInContentProvider.getInstance(this);

        final Button checkIn = (Button) findViewById(R.id.checkIn);
        checkIn.setOnClickListener(this);

        checkInList = (ListView) findViewById(R.id.checkInListView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCheckInList();
        TimePicker timePicker = (TimePicker) this.findViewById(R.id.timePicker);
        LocalDateTime now = LocalDateTime.now();
        timePicker.setHour(now.getHour());
        timePicker.setMinute(now.getMinute());
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "Clicked... ");

        // Prevent rapid button clicks
        v.setEnabled(false);

        TimePicker timePicker = (TimePicker) this.findViewById(R.id.timePicker);
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();
        final LocalDateTime newTime = LocalDateTime.now()
                .withHour(hour)
                .withMinute(minute)
                .withSecond(0)
                .withNano(0);

        Log.d(TAG, "Saving " + newTime);

        // Save check-in in background
        DATABASE_EXECUTOR.execute(() -> {
            final boolean wasSaved = checkInContentProvider.saveCheckIn(newTime);
            runOnUiThread(() -> {
                if (isDestroyed()) {
                    return;
                }
                if (!wasSaved) {
                    // Show message for duplicate check-in
                    Toast.makeText(MyTimeMainActivity.this,
                        "Check-in skipped - duplicate within 1 minute",
                        Toast.LENGTH_SHORT).show();
                }
                updateCheckInList();
                // Re-enable button after operation completes
                v.setEnabled(true);
            });
        });
    }

    private void updateCheckInList() {
        // Cancel any pending debounced updates
        debounceHandler.removeCallbacks(debouncedUpdate);

        // Schedule a new debounced update
        debounceHandler.postDelayed(debouncedUpdate, 100); // 100ms debounce
    }

    private void performUpdateCheckInList() {
        // Load data in background. execute() rather than submit(), so a failure reaches the
        // uncaught exception handler (and crash reporting) instead of vanishing into a Future
        DATABASE_EXECUTOR.execute(() -> {
            final List<LocalDateTime> allCheckIns = checkInContentProvider.getAll();
            final ImmutableListMultimap<String, LocalDateTime> result =
                    Multimaps.index(allCheckIns, input -> input.format(GROUP_BY_DATE_FORMATTER));
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    checkInList.setAdapter(new CheckInAdapter(MyTimeMainActivity.this, result));
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove any pending debounced updates
        debounceHandler.removeCallbacks(debouncedUpdate);
    }

}
