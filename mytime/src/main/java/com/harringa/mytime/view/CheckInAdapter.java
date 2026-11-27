package com.harringa.mytime.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.TtsSpan;
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
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
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
            convertView.setContentDescription(null);
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

        final boolean missingCheckIn = (firstDateTimeForView.isBefore(startOfDay) ||
                firstDateTimeForView.isAfter(endOfDay)) &&
                TimeCalculator.hasAnInstantWithoutAPair(sortedDateTimes);
        if (missingCheckIn) {
            holder.dateTotal.setTextColor(Color.RED);
            holder.dateTotal.setText(R.string.missingCheckInText);
            convertView.setContentDescription(describeDay(sortedDateTimes, null));
        } else {
            final Duration totalTime = TimeCalculator.totalTime(sortedDateTimes);
            final long totalHours = totalTime.toHours();
            if (totalHours >= 8) {
                holder.dateTotal.setTextColor(context.getColor(R.color.forest_green));
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
            convertView.setContentDescription(describeDay(sortedDateTimes, totalTime));
        }

        return convertView;
    }

    /**
     * What TalkBack reads for a day: the row speaks as one unit (its TextViews are not
     * important for accessibility), because the visible text does not read aloud well:
     * "01/05/26", "08h 30m", "09:00  17:30". The date is spelled out, the total uses words,
     * and each time carries a {@link TtsSpan} so it is read as a time of day.
     *
     * @param totalTime the day's total, or null when the day is missing a check-in
     */
    CharSequence describeDay(ImmutableList<LocalDateTime> sortedDateTimes, Duration totalTime) {
        final Resources resources = context.getResources();
        final Locale locale = resources.getConfiguration().getLocales().get(0);
        final String date = sortedDateTimes.get(0).toLocalDate()
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale));

        final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        final SpannableStringBuilder times = new SpannableStringBuilder();
        for (LocalDateTime dateTime : sortedDateTimes) {
            if (times.length() > 0) {
                times.append(", ");
            }
            final SpannableString time = new SpannableString(dateTime.format(timeFormatter));
            time.setSpan(new TtsSpan.TimeBuilder(dateTime.getHour(), dateTime.getMinute()).build(),
                    0, time.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            times.append(time);
        }

        if (totalTime == null) {
            return TextUtils.expandTemplate(resources.getText(R.string.dayMissingCheckInDescription), date, times);
        }
        return TextUtils.expandTemplate(resources.getText(R.string.dayDescription), date, describeDuration(totalTime), times);
    }

    private String describeDuration(Duration duration) {
        final Resources resources = context.getResources();
        final int hours = (int) duration.toHours();
        final int minutes = (int) (duration.toMinutes() % 60);
        final String hoursText = resources.getQuantityString(R.plurals.durationHours, hours, hours);
        final String minutesText = resources.getQuantityString(R.plurals.durationMinutes, minutes, minutes);
        if (hours == 0) {
            return minutesText;
        }
        if (minutes == 0) {
            return hoursText;
        }
        return TextUtils.expandTemplate(resources.getText(R.string.durationHoursAndMinutes), hoursText, minutesText).toString();
    }

    private ImmutableList<LocalDateTime> getSortedDateTimes(final ImmutableList<LocalDateTime> unsortedDateTimes) {
        return ImmutableList.copyOf(Ordering.natural().immutableSortedCopy(unsortedDateTimes));
    }
}
