package com.harringa.mytime.view;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.graphics.Color;
import android.text.Spanned;
import android.text.style.TtsSpan;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import com.harringa.mytime.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class CheckInAdapterTest {

    // A past day, so totals don't depend on the current time
    private static final LocalDate PAST_DAY = LocalDate.of(2026, 1, 5);

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void showsOneRowPerDate() {
        CheckInAdapter adapter = adapterFor(
                PAST_DAY.atTime(9, 0), PAST_DAY.atTime(17, 0),
                PAST_DAY.plusDays(1).atTime(9, 0), PAST_DAY.plusDays(1).atTime(17, 0));

        assertEquals(2, adapter.getCount());
        assertEquals("01/05/26", text(row(adapter, 0), R.id.checkInDate));
        assertEquals("01/06/26", text(row(adapter, 1), R.id.checkInDate));
    }

    @Test
    public void listsTheDaysCheckInTimesInOrder() {
        View row = row(adapterFor(PAST_DAY.atTime(17, 30), PAST_DAY.atTime(9, 0)), 0);

        assertEquals("09:00  17:30", text(row, R.id.checkInTimes));
    }

    @Test
    public void dayOfEightHoursOrMoreIsGreen() {
        View row = row(adapterFor(PAST_DAY.atTime(9, 0), PAST_DAY.atTime(17, 30)), 0);

        assertEquals("08h 30m", text(row, R.id.dateTotal));
        assertEquals(context.getColor(R.color.forest_green), textColor(row, R.id.dateTotal));
    }

    @Test
    public void dayUnderEightHoursIsRed() {
        View row = row(adapterFor(PAST_DAY.atTime(9, 0), PAST_DAY.atTime(12, 15)), 0);

        assertEquals("03h 15m", text(row, R.id.dateTotal));
        assertEquals(Color.RED, textColor(row, R.id.dateTotal));
    }

    @Test
    public void totalAddsUpEveryPairOfCheckIns() {
        View row = row(adapterFor(
                PAST_DAY.atTime(8, 0), PAST_DAY.atTime(12, 0),
                PAST_DAY.atTime(12, 30), PAST_DAY.atTime(17, 0)), 0);

        assertEquals("08h 30m", text(row, R.id.dateTotal));
    }

    @Test
    public void pastDayWithAnUnpairedCheckInShowsMissingCheckIn() {
        View row = row(adapterFor(PAST_DAY.atTime(9, 0), PAST_DAY.atTime(12, 0), PAST_DAY.atTime(13, 0)), 0);

        assertEquals(context.getString(R.string.missingCheckInText), text(row, R.id.dateTotal));
        assertEquals(Color.RED, textColor(row, R.id.dateTotal));
    }

    @Test
    public void talkBackReadsTheDayAsOneSpelledOutSentence() {
        View row = row(adapterFor(PAST_DAY.atTime(17, 30), PAST_DAY.atTime(9, 0)), 0);

        assertEquals("Monday, January 5, 2026. Total 8 hours 30 minutes. Times: 09:00, 17:30",
                row.getContentDescription().toString());
        for (int id : new int[] {R.id.checkInDate, R.id.dateTotal, R.id.checkInTimes}) {
            assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, row.findViewById(id).getImportantForAccessibility());
        }
    }

    @Test
    public void eachTimeIsMarkedAsATimeOfDayForTextToSpeech() {
        View row = row(adapterFor(PAST_DAY.atTime(9, 5), PAST_DAY.atTime(17, 30)), 0);
        Spanned description = (Spanned) row.getContentDescription();

        TtsSpan[] spans = description.getSpans(0, description.length(), TtsSpan.class);
        assertEquals(2, spans.length);
        assertArrayEquals(new String[] {"09:05", "17:30"}, new String[] {spanned(description, spans[0]), spanned(description, spans[1])});
        assertEquals(TtsSpan.TYPE_TIME, spans[0].getType());
        assertEquals(9, spans[0].getArgs().getInt(TtsSpan.ARG_HOURS));
        assertEquals(5, spans[0].getArgs().getInt(TtsSpan.ARG_MINUTES));
        assertEquals(17, spans[1].getArgs().getInt(TtsSpan.ARG_HOURS));
        assertEquals(30, spans[1].getArgs().getInt(TtsSpan.ARG_MINUTES));
    }

    @Test
    public void talkBackSaysAPastDayIsMissingACheckIn() {
        View row = row(adapterFor(PAST_DAY.atTime(9, 0), PAST_DAY.atTime(12, 0), PAST_DAY.atTime(13, 0)), 0);

        assertEquals("Monday, January 5, 2026. Missing check in. Times: 09:00, 12:00, 13:00",
                row.getContentDescription().toString());
    }

    @Test
    public void spokenTotalUsesWordsAndDropsAZeroPart() {
        assertEquals("Total 8 hours.", spokenTotal(PAST_DAY.atTime(9, 0), PAST_DAY.atTime(17, 0)));
        assertEquals("Total 45 minutes.", spokenTotal(PAST_DAY.atTime(9, 0), PAST_DAY.atTime(9, 45)));
        assertEquals("Total 1 hour 1 minute.", spokenTotal(PAST_DAY.atTime(9, 0), PAST_DAY.atTime(10, 1)));
        assertEquals("Total 0 minutes.", spokenTotal(PAST_DAY.atTime(9, 0), PAST_DAY.atTime(9, 0, 30)));
    }

    private String spokenTotal(LocalDateTime in, LocalDateTime out) {
        String description = row(adapterFor(in, out), 0).getContentDescription().toString();
        return description.substring(description.indexOf("Total"), description.indexOf(" Times:"));
    }

    private static String spanned(Spanned text, Object span) {
        return text.subSequence(text.getSpanStart(span), text.getSpanEnd(span)).toString();
    }

    private CheckInAdapter adapterFor(LocalDateTime... checkIns) {
        ImmutableListMultimap<String, LocalDateTime> byDate =
                Multimaps.index(Arrays.asList(checkIns), checkIn -> checkIn.toLocalDate().toString());
        return new CheckInAdapter(context, byDate);
    }

    private View row(CheckInAdapter adapter, int position) {
        return adapter.getView(position, null, new FrameLayout(context));
    }

    private static String text(View row, int id) {
        return ((TextView) row.findViewById(id)).getText().toString();
    }

    private static int textColor(View row, int id) {
        return ((TextView) row.findViewById(id)).getCurrentTextColor();
    }
}
