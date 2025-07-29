package com.harringa.mytime;

import android.app.Activity;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import com.harringa.mytime.repository.CheckInContentProvider;
import com.harringa.mytime.view.CheckInAdapter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

public class MyTimeMainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "MyTimeMainActivity";
    private CheckInContentProvider checkInContentProvider;
    private ListView checkInList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        checkInContentProvider = CheckInContentProvider.getInstance(this);

        final Button checkIn = (Button) findViewById(R.id.checkIn);
        checkIn.setOnClickListener(this);

        checkInList = (ListView) findViewById(R.id.checkInListView);
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.checkInList = (ListView) findViewById(R.layout.check_in_list);
        viewHolder.checkInDate = (TextView) findViewById(R.id.checkInDate);
        checkInList.setTag(viewHolder);
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
        checkInContentProvider.saveCheckIn(newTime);
        updateCheckInList();
    }

    private void updateCheckInList() {
        final ImmutableListMultimap<Integer, LocalDateTime> instantsGroupedByDate =
                Multimaps.index(checkInContentProvider.getAll(), new Function<LocalDateTime, Integer>() {
                    @Override
                    public Integer apply(LocalDateTime input) {
                        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        return Integer.valueOf(input.format(dateFormatter));
                    }
                });
        checkInList.setAdapter(new CheckInAdapter(this, instantsGroupedByDate));
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
