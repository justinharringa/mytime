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
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;

public class CheckInAdapter extends BaseAdapter {

    private static final String TAG = "CheckInAdapter";
    private final Context context;
    private final ImmutableListMultimap<String, LocalDateTime> dateTimesGroupedByDate;

    // Static ViewHolder class for view recycling
    private static class ViewHolder {
        TextView checkInDate;
        TextView checkInTimes;
        TextView dateTotal;
    }

    public CheckInAdapter(Context context, ImmutableListMultimap<String, LocalDateTime> dateTimesGroupedByDate) {
        this.context = context;
        this.dateTimesGroupedByDate = dateTimesGroupedByDate;
    }

    @Override
    public int getCount() {
        return dateTimesGroupedByDate.keySet().size();
    }

    @Override
    public ImmutableList<LocalDateTime> getItem(int position) {
        final String key = dateTimesGroupedByDate.keySet().asList().get(position);
        return dateTimesGroupedByDate.get(key);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            // Inflate new view and create ViewHolder
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.check_in_list, parent, false);

            holder = new ViewHolder();
            holder.checkInDate = (TextView) convertView.findViewById(R.id.checkInDate);
            holder.checkInTimes = (TextView) convertView.findViewById(R.id.checkInTimes);
            holder.dateTotal = (TextView) convertView.findViewById(R.id.dateTotal);

            convertView.setTag(holder);
        } else {
            // Reuse existing view and ViewHolder
            holder = (ViewHolder) convertView.getTag();
        }

        // Get data for this position
        ImmutableList<LocalDateTime> sortedDateTimes = getSortedDateTimes(getItem(position));
        if (sortedDateTimes.isEmpty()) {
            return convertView;
        }

        final LocalDateTime firstDateTimeForView = sortedDateTimes.get(0);

        // Update date
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yy");
        holder.checkInDate.setText(firstDateTimeForView.format(dateFormatter));

        // Update times - optimize string building
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        final StringBuilder stringBuilder = new StringBuilder(sortedDateTimes.size() * 6); // Estimate 6 chars per time
        for (LocalDateTime dateTime : sortedDateTimes) {
            stringBuilder.append(dateTime.format(timeFormatter)).append("  ");
        }
        holder.checkInTimes.setText(stringBuilder.toString().trim());

        // Update total time - optimize calculation
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        if ((firstDateTimeForView.isBefore(startOfDay) ||
                firstDateTimeForView.isAfter(endOfDay)) &&
                TimeCalculator.hasAnInstantWithoutAPair(sortedDateTimes)) {
            holder.dateTotal.setTextColor(Color.RED);
            holder.dateTotal.setText(R.string.missingCheckInText);
        } else {
            final java.time.Duration totalTime = TimeCalculator.totalTime(sortedDateTimes);
            final long totalHours = totalTime.toHours();
            if (totalHours >= 8) {
                holder.dateTotal.setTextColor(context.getResources().getColor(R.color.forest_green));
            } else {
                holder.dateTotal.setTextColor(Color.RED);
            }
            final long totalMinutes = totalTime.toMinutes();
            final long minutesPart = totalMinutes % 60;
            final String totalString = String.format(Locale.ENGLISH,
                    "%02dh %02dm",
                    totalHours,
                    minutesPart);
            holder.dateTotal.setText(totalString);
        }

        return convertView;
    }

    private ImmutableList<LocalDateTime> getSortedDateTimes(final ImmutableList<LocalDateTime> unsortedDateTimes) {
        return ImmutableList.copyOf(Ordering.natural().immutableSortedCopy(unsortedDateTimes));
    }
}
