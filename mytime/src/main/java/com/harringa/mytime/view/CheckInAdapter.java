package com.harringa.mytime.view;

import android.content.Context;
import android.graphics.Color;
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
import com.harringa.mytime.math.TimeCalculator;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.ISODateTimeFormat;

import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

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

        DateTimeFormatter dateFormatter = DateTimeFormat.shortDate()
                .withZone(DateTimeZone.forTimeZone(TimeZone.getDefault()));
        TextView checkInDate = (TextView) view.findViewById(R.id.checkInDate);
        final Instant firstInstantForView = sortedInstants.get(0);
        checkInDate.setText(firstInstantForView.toString(dateFormatter));

        DateTimeFormatter timeFormatter = DateTimeFormat.shortTime()
                .withZone(DateTimeZone.forTimeZone(TimeZone.getDefault()));

        TextView checkInTimes = (TextView) view.findViewById(R.id.checkInTimes);
        final StringBuilder stringBuilder = new StringBuilder();
        for (Instant instant : sortedInstants) {
            Log.d(TAG, "instant: " + instant);
            stringBuilder.append(instant.toString(timeFormatter))
                    .append("  ");
        }
        checkInTimes.setText(stringBuilder.toString().trim());

        TextView dateTotal = (TextView) view.findViewById(R.id.dateTotal);
        if ((firstInstantForView.isBefore(DateTime.now().dayOfMonth().roundFloorCopy()) ||
                firstInstantForView.isAfter(DateTime.now().dayOfMonth().roundCeilingCopy())) &&
                TimeCalculator.hasAnInstantWithoutAPair(sortedInstants)) {
            dateTotal.setTextColor(Color.RED);
            dateTotal.setText(R.string.missingCheckInText);

        } else {

            final Period totalTime = TimeCalculator.totalTime(sortedInstants);
            final int totalHours = totalTime.getHours();
            Log.d(TAG, "totalHours: " + totalHours);
            if (totalHours >= 8) {
                dateTotal.setTextColor(context.getResources().getColor(R.color.forest_green));
            } else {
                dateTotal.setTextColor(Color.RED);
            }
            final String totalString = String.format(Locale.ENGLISH,
                    "%02dh %02dm",
                    totalHours,
                    totalTime.getMinutes());
            dateTotal.setText(totalString);
        }

        return view;
    }

    private ImmutableList<Instant> getSortedInstants(final ImmutableList<Instant> unsortedInstants) {
        return ImmutableList.copyOf(Ordering.natural().immutableSortedCopy(unsortedInstants));
    }
}
