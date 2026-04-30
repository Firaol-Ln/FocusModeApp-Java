package com.example.focusmodejv;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.focusmodejv.R;
import com.example.focusmodejv.data.DatabaseHelper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StatsActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    private TextView tabDay, tabWeek, tabMonth, tabYear;
    private TextView tvCurrentDate, tvTotalDuration, tvCurrentDuration;
    private ImageButton btnPrev, btnNext;
    private ProgressBar progressBar;
    private LineChart lineChart;

    private DatabaseHelper dbHelper;
    private Calendar currentCalendar;

    private enum Mode { DAY, WEEK, MONTH, YEAR }
    private Mode currentMode = Mode.DAY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        dbHelper = new DatabaseHelper(this);
        currentCalendar = Calendar.getInstance();

        initViews();
        setupChart();
        setupListeners();

        setMode(Mode.DAY); // Initial mode
    }

    private void initViews() {
        tabDay = findViewById(R.id.tabDay);
        tabWeek = findViewById(R.id.tabWeek);
        tabMonth = findViewById(R.id.tabMonth);
        tabYear = findViewById(R.id.tabYear);

        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        tvTotalDuration = findViewById(R.id.tvTotalDuration);
        tvCurrentDuration = findViewById(R.id.tvCurrentDuration);

        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);

        progressBar = findViewById(R.id.progressBar);
        lineChart = findViewById(R.id.lineChart);
    }

    private void setupChart() {
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(false);
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setBackgroundColor(Color.TRANSPARENT);
        lineChart.setDrawGridBackground(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.LTGRAY);
        xAxis.setGridLineWidth(0.5f);
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineColor(Color.LTGRAY);
        xAxis.setTextColor(Color.GRAY);
        xAxis.setGranularity(1f);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setGridLineWidth(0.5f);
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setTextColor(Color.GRAY);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setXOffset(10f);

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void setupListeners() {
        tabDay.setOnClickListener(v -> setMode(Mode.DAY));
        tabWeek.setOnClickListener(v -> setMode(Mode.WEEK));
        tabMonth.setOnClickListener(v -> setMode(Mode.MONTH));
        tabYear.setOnClickListener(v -> setMode(Mode.YEAR));

        btnPrev.setOnClickListener(v -> shiftDate(-1));
        btnNext.setOnClickListener(v -> shiftDate(1));

        tvCurrentDate.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            currentCalendar.set(Calendar.YEAR, year);
            currentCalendar.set(Calendar.MONTH, month);
            currentCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateUI();
        }, currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH), currentCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setMode(Mode mode) {
        currentMode = mode;
        
        // Update Tab Selection UI
        tabDay.setBackgroundResource(mode == Mode.DAY ? R.drawable.tab_bg_selected : 0);
        tabDay.setTextColor(mode == Mode.DAY ? getResources().getColor(R.color.text_primary) : getResources().getColor(R.color.text_secondary));

        tabWeek.setBackgroundResource(mode == Mode.WEEK ? R.drawable.tab_bg_selected : 0);
        tabWeek.setTextColor(mode == Mode.WEEK ? getResources().getColor(R.color.text_primary) : getResources().getColor(R.color.text_secondary));

        tabMonth.setBackgroundResource(mode == Mode.MONTH ? R.drawable.tab_bg_selected : 0);
        tabMonth.setTextColor(mode == Mode.MONTH ? getResources().getColor(R.color.text_primary) : getResources().getColor(R.color.text_secondary));

        tabYear.setBackgroundResource(mode == Mode.YEAR ? R.drawable.tab_bg_selected : 0);
        tabYear.setTextColor(mode == Mode.YEAR ? getResources().getColor(R.color.text_primary) : getResources().getColor(R.color.text_secondary));

        updateUI();
    }

    private void shiftDate(int amount) {
        switch (currentMode) {
            case DAY:
                currentCalendar.add(Calendar.DAY_OF_YEAR, amount);
                break;
            case WEEK:
                currentCalendar.add(Calendar.DAY_OF_YEAR, amount * 7);
                break;
            case MONTH:
                currentCalendar.add(Calendar.MONTH, amount);
                break;
            case YEAR:
                currentCalendar.add(Calendar.YEAR, amount);
                break;
        }
        updateUI();
    }

    private void updateUI() {
        SimpleDateFormat sdf;
        switch (currentMode) {
            case DAY:
                sdf = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
                tvCurrentDate.setText(sdf.format(currentCalendar.getTime()));
                break;
            case WEEK:
                Calendar startOfWeek = (Calendar) currentCalendar.clone();
                startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                Calendar endOfWeek = (Calendar) startOfWeek.clone();
                endOfWeek.add(Calendar.DAY_OF_YEAR, 6);
                sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
                String weekStr = sdf.format(startOfWeek.getTime()) + " - " + sdf.format(endOfWeek.getTime());
                tvCurrentDate.setText(weekStr);
                break;
            case MONTH:
                sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                tvCurrentDate.setText(sdf.format(currentCalendar.getTime()));
                break;
            case YEAR:
                sdf = new SimpleDateFormat("yyyy", Locale.getDefault());
                tvCurrentDate.setText(sdf.format(currentCalendar.getTime()));
                break;
        }

        long startMs = getStartOfPeriod(currentCalendar, currentMode).getTimeInMillis();
        long endMs = getEndOfPeriod(currentCalendar, currentMode).getTimeInMillis();

        List<DatabaseHelper.Session> sessions = dbHelper.getSessionsInRange(startMs, endMs);

        long totalDurationMs = 0;
        for (DatabaseHelper.Session s : sessions) {
            totalDurationMs += s.durationMs;
        }

        tvTotalDuration.setText(formatDisplayDuration(totalDurationMs));
        
        // Calculate "Today" focus time regardless of view
        long todayStartMs = getStartOfPeriod(Calendar.getInstance(), Mode.DAY).getTimeInMillis();
        long todayEndMs = getEndOfPeriod(Calendar.getInstance(), Mode.DAY).getTimeInMillis();
        List<DatabaseHelper.Session> todaySessions = dbHelper.getSessionsInRange(todayStartMs, todayEndMs);
        long todayDurationMs = 0;
        for (DatabaseHelper.Session s : todaySessions) todayDurationMs += s.durationMs;
        tvCurrentDuration.setText(formatDisplayDuration(todayDurationMs));

        long goalMs = 120 * 60 * 1000L; // 2 hour daily goal
        int progress = (int) Math.min(100, (todayDurationMs * 100) / goalMs);
        progressBar.setProgress(progress);

        plotData(sessions);
    }

    private void plotData(List<DatabaseHelper.Session> sessions) {
        List<Entry> entries = new ArrayList<>();
        final String[] labels;
        
        if (currentMode == Mode.DAY) {
            float[] hourBuckets = new float[24];
            for (DatabaseHelper.Session s : sessions) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(s.endTimeMs);
                int hour = c.get(Calendar.HOUR_OF_DAY);
                hourBuckets[hour] += (s.durationMs / 60000f);
            }
            for (int i = 0; i < 24; i++) {
                entries.add(new Entry(i, hourBuckets[i]));
            }
            labels = new String[]{"00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23"};
        } else if (currentMode == Mode.WEEK) {
            float[] dayBuckets = new float[7];
            for (DatabaseHelper.Session s : sessions) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(s.endTimeMs);
                int day = (c.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7;
                dayBuckets[day] += (s.durationMs / 60000f);
            }
            for (int i = 0; i < 7; i++) {
                entries.add(new Entry(i, dayBuckets[i]));
            }
            labels = new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        } else if (currentMode == Mode.MONTH) {
            int daysInMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            float[] dayBuckets = new float[daysInMonth];
            for (DatabaseHelper.Session s : sessions) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(s.endTimeMs);
                int day = c.get(Calendar.DAY_OF_MONTH) - 1;
                if (day >= 0 && day < daysInMonth) {
                    dayBuckets[day] += (s.durationMs / 60000f);
                }
            }
            labels = new String[daysInMonth];
            for (int i = 0; i < daysInMonth; i++) {
                entries.add(new Entry(i, dayBuckets[i]));
                labels[i] = String.valueOf(i + 1);
            }
        } else {
            float[] monthBuckets = new float[12];
            for (DatabaseHelper.Session s : sessions) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(s.endTimeMs);
                int month = c.get(Calendar.MONTH);
                monthBuckets[month] += (s.durationMs / 60000f);
            }
            labels = new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            for (int i = 0; i < 12; i++) {
                entries.add(new Entry(i, monthBuckets[i]));
            }
        }

        lineChart.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < labels.length) {
                    if (currentMode == Mode.DAY) {
                        // Only show labels for 00, 04, 08, 12, 16, 20
                        return (index % 4 == 0) ? labels[index] : "";
                    }
                    return labels[index];
                }
                return "";
            }
        });

        LineDataSet dataSet = new LineDataSet(entries, "Focus Time");
        dataSet.setColor(Color.parseColor("#4DB6AC"));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleColor(Color.parseColor("#4DB6AC"));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleColor(getResources().getColor(R.color.card_bg));
        dataSet.setCircleHoleRadius(2f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        
        // Gradient fill
        dataSet.setFillDrawable(null);
        dataSet.setFillColor(Color.parseColor("#4DB6AC"));
        dataSet.setFillAlpha(40);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.animateX(800);
        lineChart.invalidate();
    }

    private String formatDisplayDuration(long millis) {
        int seconds = (int) (millis / 1000);
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%dH %02dM", hours, minutes);
        } else if (minutes > 0) {
            return String.format(Locale.getDefault(), "%dM %02dS", minutes, secs);
        } else {
            return String.format(Locale.getDefault(), "0H %02dS", secs);
        }
    }

    private Calendar getStartOfPeriod(Calendar current, Mode mode) {
        Calendar cal = (Calendar) current.clone();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (mode == Mode.WEEK) {
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        } else if (mode == Mode.MONTH) {
            cal.set(Calendar.DAY_OF_MONTH, 1);
        } else if (mode == Mode.YEAR) {
            cal.set(Calendar.DAY_OF_YEAR, 1);
        }
        return cal;
    }

    private Calendar getEndOfPeriod(Calendar current, Mode mode) {
        Calendar cal = getStartOfPeriod(current, mode);
        if (mode == Mode.DAY) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        } else if (mode == Mode.WEEK) {
            cal.add(Calendar.DAY_OF_YEAR, 7);
        } else if (mode == Mode.MONTH) {
            cal.add(Calendar.MONTH, 1);
        } else if (mode == Mode.YEAR) {
            cal.add(Calendar.YEAR, 1);
        }
        cal.add(Calendar.MILLISECOND, -1);
        return cal;
    }
}
