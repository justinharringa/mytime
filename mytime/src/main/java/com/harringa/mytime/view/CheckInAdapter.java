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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public class CheckInAdapter extends BaseAdapter {

    private static final String TAG = "CheckInAdapter";
    private final Context context;
    private final ImmutableListMultimap<Integer, LocalDateTime> dateTimesGroupedByDate;

    public CheckInAdapter(Context context, ImmutableListMultimap<Integer, LocalDateTime> dateTimesGroupedByDate) {
        this.context = context;
        this.dateTimesGroupedByDate = dateTimesGroupedByDate;
    }

    @Override
    public int getCount() {
        return dateTimesGroupedByDate.keySet().size();
    }

    @Override
    public ImmutableList<LocalDateTime> getItem(int position) {
        final Integer key = dateTimesGroupedByDate.keySet().asList().get(position);
        return dateTimesGroupedByDate.get(key);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImmutableList<LocalDateTime> sortedDateTimes = getSortedDateTimes(getItem(position));

        Log.d(TAG, "getView() adapter");
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.check_in_list, null);

        checkNotNull(view);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yy");
        TextView checkInDate = (TextView) view.findViewById(R.id.checkInDate);
        final LocalDateTime firstDateTimeForView = sortedDateTimes.get(0);
        checkInDate.setText(firstDateTimeForView.format(dateFormatter));

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        TextView checkInTimes = (TextView) view.findViewById(R.id.checkInTimes);
        final StringBuilder stringBuilder = new StringBuilder();
        for (LocalDateTime dateTime : sortedDateTimes) {
            Log.d(TAG, "dateTime: " + dateTime);
            stringBuilder.append(dateTime.format(timeFormatter))
                    .append("  ");
        }
        checkInTimes.setText(stringBuilder.toString().trim());

        TextView dateTotal = (TextView) view.findViewById(R.id.dateTotal);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        if ((firstDateTimeForView.isBefore(startOfDay) ||
                firstDateTimeForView.isAfter(endOfDay)) &&
                TimeCalculator.hasAnInstantWithoutAPair(sortedDateTimes)) {
            dateTotal.setTextColor(Color.RED);
            dateTotal.setText(R.string.missingCheckInText);

        } else {

            final java.time.Duration totalTime = TimeCalculator.totalTime(sortedDateTimes);
            final long totalHours = totalTime.toHours();
            Log.d(TAG, "totalHours: " + totalHours);
            if (totalHours >= 8) {
                dateTotal.setTextColor(context.getResources().getColor(R.color.forest_green));
            } else {
                dateTotal.setTextColor(Color.RED);
            }
            final String totalString = String.format(Locale.ENGLISH,
                    "%02dh %02dm",
                    totalHours,
                    totalTime.toMinutesPart());
            dateTotal.setText(totalString);
        }

        return view;
    }

    private ImmutableList<LocalDateTime> getSortedDateTimes(final ImmutableList<LocalDateTime> unsortedDateTimes) {
        return ImmutableList.copyOf(Ordering.natural().immutableSortedCopy(unsortedDateTimes));
    }
}
