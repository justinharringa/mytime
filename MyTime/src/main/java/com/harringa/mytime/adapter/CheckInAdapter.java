package com.harringa.mytime.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.harringa.mytime.R;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.ISODateTimeFormat;

import java.util.List;
import java.util.TimeZone;

import static com.google.common.base.Preconditions.checkNotNull;

public class CheckInAdapter extends BaseAdapter {

    private static final String TAG = "CheckInAdapter";
    private final Context context;
    private final List<Instant> instants;

    public CheckInAdapter(Context context, List<Instant> instants) {
        this.context = context;
        this.instants = instants;
    }
    @Override
    public int getCount() {
        return instants.size();
    }

    @Override
    public Object getItem(int position) {
        return instants.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Instant instant = instants.get(position);
        Log.d(TAG, "getView() adapter - " + instant.getMillis());
        Log.d(TAG, "getView() year - " + instant.get(DateTimeFieldType.year()));
        Log.d(TAG, "getView() month - " + instant.get(DateTimeFieldType.monthOfYear()));
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.check_in_list, null);

        checkNotNull(view);

        DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
                .append(ISODateTimeFormat.date())
                .appendLiteral(" at ")
                .append(ISODateTimeFormat.hourMinute())
                .toFormatter()
                .withZone(DateTimeZone.forTimeZone(TimeZone.getDefault()));

        TextView checkInDate = (TextView) view.findViewById(R.id.checkInDate);
        checkInDate.setText(instant.toString(dateTimeFormatter));

        return view;
    }
}
