package com.harringa.mytime.view;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Ordering;
import com.harringa.mytime.R;
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
    private final ImmutableListMultimap<Integer, Instant> instantsGroupedByDate;

    public CheckInAdapter(Context context, ImmutableListMultimap<Integer, Instant> instantsGroupedByDate) {
        this.context = context;
        this.instantsGroupedByDate = instantsGroupedByDate;
    }
    @Override
    public int getCount() {
        return instantsGroupedByDate.keySet().size();
    }

    @Override
    public ImmutableList<Instant> getItem(int position) {
        final Integer key = instantsGroupedByDate.keySet().asList().get(position);
        return instantsGroupedByDate.get(key);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImmutableList<Instant> sortedInstants = getSortedInstants(getItem(position));

        Log.d(TAG, "getView() adapter");
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.check_in_list, null);

        checkNotNull(view);

        DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder()
                .append(ISODateTimeFormat.date())
                .toFormatter()
                .withZone(DateTimeZone.forTimeZone(TimeZone.getDefault()));
        TextView checkInDate = (TextView) view.findViewById(R.id.checkInDate);
        checkInDate.setText(sortedInstants.get(0).toString(dateFormatter));

        DateTimeFormatter timeFormatter = new DateTimeFormatterBuilder()
                .append(ISODateTimeFormat.hourMinute())
                .toFormatter()
                .withZone(DateTimeZone.forTimeZone(TimeZone.getDefault()));

        TextView checkInTimes = (TextView) view.findViewById(R.id.checkInTimes);
        final StringBuilder stringBuilder = new StringBuilder();
        for (Instant instant : sortedInstants) {
            stringBuilder.append(instant.toString(timeFormatter))
                .append("  ");
        }
        checkInTimes.setText(stringBuilder.toString().trim());


        return view;
    }

    private ImmutableList<Instant> getSortedInstants(final ImmutableList<Instant> unsortedInstants) {
        return ImmutableList.copyOf(Ordering.natural().immutableSortedCopy(unsortedInstants));
    }
}
