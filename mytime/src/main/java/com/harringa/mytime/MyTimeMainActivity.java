package com.harringa.mytime;

import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import com.harringa.mytime.repository.CheckInContentProvider;
import com.harringa.mytime.view.CheckInAdapter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MyTimeMainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "MyTimeMainActivity";
    private CheckInContentProvider checkInContentProvider;
    private ListView checkInList;
    private AsyncTask<Void, Void, ImmutableListMultimap<String, LocalDateTime>> updateTask;
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            timePicker.setHour(now.getHour());
            timePicker.setMinute(now.getMinute());
        } else {
            timePicker.setCurrentHour(now.getHour());
            timePicker.setCurrentMinute(now.getMinute());
        }
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "Clicked... ");

        // Prevent rapid button clicks
        v.setEnabled(false);

        TimePicker timePicker = (TimePicker) this.findViewById(R.id.timePicker);
        int hour, minute;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            hour = timePicker.getHour();
            minute = timePicker.getMinute();
        } else {
            hour = timePicker.getCurrentHour();
            minute = timePicker.getCurrentMinute();
        }
        final LocalDateTime newTime = LocalDateTime.now()
                .withHour(hour)
                .withMinute(minute)
                .withSecond(0)
                .withNano(0);

        Log.d(TAG, "Saving " + newTime);

        // Save check-in in background
        new AsyncTask<LocalDateTime, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(LocalDateTime... params) {
                return checkInContentProvider.saveCheckIn(params[0]);
            }

            @Override
            protected void onPostExecute(Boolean wasSaved) {
                if (!wasSaved) {
                    // Show message for duplicate check-in
                    Toast.makeText(MyTimeMainActivity.this, 
                        "Check-in skipped - duplicate within 1 minute", 
                        Toast.LENGTH_SHORT).show();
                }
                updateCheckInList();
                // Re-enable button after operation completes
                v.setEnabled(true);
            }
        }.execute(newTime);
    }

    private void updateCheckInList() {
        // Cancel any pending debounced updates
        debounceHandler.removeCallbacks(debouncedUpdate);

        // Schedule a new debounced update
        debounceHandler.postDelayed(debouncedUpdate, 100); // 100ms debounce
    }

    private void performUpdateCheckInList() {
        // Cancel any existing update task
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel(true);
        }

        // Load data in background
        updateTask = new AsyncTask<Void, Void, ImmutableListMultimap<String, LocalDateTime>>() {
            @Override
            protected ImmutableListMultimap<String, LocalDateTime> doInBackground(Void... params) {
                if (isCancelled()) {
                    return null;
                }
                final List<LocalDateTime> allCheckIns = checkInContentProvider.getAll();
                if (isCancelled()) {
                    return null;
                }
                return Multimaps.index(allCheckIns, new Function<LocalDateTime, String>() {
                    @Override
                    public String apply(LocalDateTime input) {
                        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        return input.format(dateFormatter);
                    }
                });
            }

            @Override
            protected void onPostExecute(ImmutableListMultimap<String, LocalDateTime> result) {
                if (result != null && !isFinishing()) {
                    checkInList.setAdapter(new CheckInAdapter(MyTimeMainActivity.this, result));
                }
            }
        };
        updateTask.execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Cancel any pending tasks
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel(true);
        }

        // Remove any pending debounced updates
        debounceHandler.removeCallbacks(debouncedUpdate);

        if (checkInContentProvider != null) {
            checkInContentProvider.close();
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_main, container, false);
        }
    }

}
