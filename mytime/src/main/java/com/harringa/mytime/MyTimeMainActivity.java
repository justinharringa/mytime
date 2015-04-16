package com.harringa.mytime;

import android.app.Activity;
import android.app.Fragment;
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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.ISODateTimeFormat;

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
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCheckInList();
        TimePicker timePicker = (TimePicker) this.findViewById(R.id.timePicker);
        timePicker.setCurrentHour(DateTime.now().getHourOfDay());
        timePicker.setCurrentMinute(DateTime.now().getMinuteOfHour());
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "Clicked... ");
        TimePicker timePicker = (TimePicker) this.findViewById(R.id.timePicker);
        final DateTime newTime = DateTime.now()
                .withHourOfDay(timePicker.getCurrentHour())
                .withMinuteOfHour(timePicker.getCurrentMinute())
                .withSecondOfMinute(0)
                .withMillisOfSecond(0);

        Log.d(TAG, "Saving " + newTime);
        checkInContentProvider.saveCheckIn(newTime);
        updateCheckInList();
    }

    private void updateCheckInList() {
        final ImmutableListMultimap<Integer, Instant> instantsGroupedByDate =
                Multimaps.index(checkInContentProvider.getAll(), new Function<Instant, Integer>() {
                    @Override
                    public Integer apply(Instant input) {
                        DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder()
                                .append(ISODateTimeFormat.basicDate())
                                .toFormatter()
                                .withZone(DateTimeZone.forTimeZone(TimeZone.getDefault()));
                        return Integer.valueOf(input.toString(dateFormatter));
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
