package com.example.focusmodejv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.focusmodejv.R;
import com.example.focusmodejv.data.DatabaseHelper;
import com.example.focusmodejv.timer.StopwatchManager;
import com.example.focusmodejv.timer.TimerBottomSheet;
import com.example.focusmodejv.timer.TimerManager;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvMinutesTopNext, tvMinutesBottom, tvMinutesFlip;
    private TextView tvSecondsTopNext, tvSecondsBottom, tvSecondsFlip;
    private View flMinutesFlip, flSecondsFlip, vMinutesShadow, vSecondsShadow;
    private TextView tvMilliseconds;
    private ImageButton btnStart, btnCategory, btnStats, btnReset;

    private TimerManager timerManager;
    private StopwatchManager stopwatchManager;
    private DatabaseHelper dbHelper;

    private long currentFocusDuration = 25 * 60 * 1000L;
    private long defaultTime = 25 * 60 * 1000L;
    
    private String lastSecond = "00";
    private String lastMinute = "25";

    private android.net.Uri selectedSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);

    private ActivityResultLauncher<Intent> launcher;

    private boolean isAppInForeground = false;
    private boolean isAlarmRinging = false;
    private boolean isDndEnabledByApp = false;
    
    private int totalLoops = 1;
    private int remainingLoops = 1;
    private long breakDurationMs = 5 * 60 * 1000L;

    private enum Mode { FOCUS, BREAK }
    private Mode currentMode = Mode.FOCUS;

    private enum AppMode { TIMER, STOPWATCH }
    private AppMode currentAppMode = AppMode.TIMER;

    private long sessionStartTimeMs = 0;
    private long accumulatedSessionDurationMs = 0;
    private MediaPlayer currentMediaPlayer;
    private Ringtone currentRingtone;
    private AlertDialog alarmDialog;

    private static final String CHANNEL_ID = "focus_alarm_channel";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyStoredSettings();
        applyBrightness();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        
        android.content.SharedPreferences prefs = getSharedPreferences("TimerPrefs", android.content.Context.MODE_PRIVATE);
        
        // Load Default Durations
        int defaultMinutes = prefs.getInt("default_minutes", 25);
        defaultTime = defaultMinutes * 60 * 1000L;
        currentFocusDuration = defaultTime;
        
        int breakMinutes = prefs.getInt("break_minutes", 5);
        breakDurationMs = breakMinutes * 60 * 1000L;

        String savedUriStr = prefs.getString("sound_uri", null);
        if (savedUriStr != null) {
            selectedSoundUri = android.net.Uri.parse(savedUriStr);
        }
        
        createNotificationChannel();

        // Minutes views
        tvMinutesTopNext = findViewById(R.id.tvMinutesTopNext);
        tvMinutesBottom = findViewById(R.id.tvMinutesBottom);
        tvMinutesFlip = findViewById(R.id.tvMinutesFlip);
        flMinutesFlip = findViewById(R.id.flMinutesFlip);
        vMinutesShadow = findViewById(R.id.vMinutesFlipShadow);

        // Seconds views
        tvSecondsTopNext = findViewById(R.id.tvSecondsTopNext);
        tvSecondsBottom = findViewById(R.id.tvSecondsBottom);
        tvSecondsFlip = findViewById(R.id.tvSecondsFlip);
        flSecondsFlip = findViewById(R.id.flSecondsFlip);
        vSecondsShadow = findViewById(R.id.vSecondsFlipShadow);

        tvMilliseconds = findViewById(R.id.tvMilliseconds);
        btnStart = findViewById(R.id.btnStart);
        btnCategory = findViewById(R.id.btnCategories);
        btnStats = findViewById(R.id.btnStats);

        btnCategory.setImageResource(R.drawable.ic_settings);
        btnCategory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            launcher.launch(intent);
        });

        android.widget.LinearLayout timerLabelLayout = findViewById(R.id.timerLabelLayout);
        if (timerLabelLayout != null) {
            timerLabelLayout.setOnClickListener(v -> {
                TimerBottomSheet bottomSheet = new TimerBottomSheet();
                bottomSheet.setListener((hours, minutes, seconds, color, tag, soundUri, loops, brkMinutes) -> {
                    long newDuration = (hours * 3600L + minutes * 60L + seconds) * 1000L;
                    if (newDuration > 0) {
                        currentFocusDuration = newDuration;
                        defaultTime = currentFocusDuration;
                        timerManager.reset(currentFocusDuration);
                        updateUI(currentFocusDuration, false);
                    }
                    
                    this.totalLoops = loops;
                    this.remainingLoops = loops;
                    this.breakDurationMs = brkMinutes * 60 * 1000L;
                    this.currentMode = Mode.FOCUS;

                    View indicator = findViewById(R.id.timerIndicator);
                    if (indicator != null) indicator.setBackgroundColor(color);
                    TextView label = findViewById(R.id.timerLabel);
                    if (label != null) {
                        String loopText = loops > 1 ? " (" + loops + " loops)" : "";
                        label.setText(tag + loopText + " >");
                    }
                    
                    selectedSoundUri = soundUri;
                });
                bottomSheet.show(getSupportFragmentManager(), "TimerBottomSheet");
            });
        }

        btnReset = new ImageButton(this);
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                (int) (56 * getResources().getDisplayMetrics().density),
                (int) (56 * getResources().getDisplayMetrics().density)
        );
        btnReset.setLayoutParams(params);
        btnReset.setBackgroundResource(R.drawable.timer_card_bg);
        btnReset.setImageResource(android.R.drawable.ic_popup_sync);
        btnReset.setColorFilter(getResources().getColor(R.color.icon_tint), android.graphics.PorterDuff.Mode.SRC_IN);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        btnReset.setPadding(padding, padding, padding, padding);
        btnReset.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        btnReset.setVisibility(View.GONE);
        btnReset.setContentDescription("Reset");

        android.widget.LinearLayout bottomControls = findViewById(R.id.bottomControls);
        bottomControls.addView(btnReset, 0);

        btnReset.setOnClickListener(v -> {
            if (currentAppMode == AppMode.TIMER) {
                timerManager.reset(currentFocusDuration);
                updateUI(currentFocusDuration, false);
            } else {
                long elapsed = stopwatchManager.getElapsedTime();
                if (elapsed > 1000) { // Only save if more than 1 second
                    dbHelper.addSession(System.currentTimeMillis() - elapsed, System.currentTimeMillis(), elapsed);
                    Toast.makeText(this, "Session saved!", Toast.LENGTH_SHORT).show();
                }
                stopwatchManager.reset();
                updateUI(0, false);
            }
            disableDND();
            accumulatedSessionDurationMs = 0;
            btnStart.setImageResource(R.drawable.ic_play);
            btnReset.setVisibility(View.GONE);
            btnCategory.setVisibility(View.VISIBLE);
            btnStats.setVisibility(View.VISIBLE);
        });

        timerManager = new TimerManager(defaultTime);
        stopwatchManager = new StopwatchManager();

        TextView tabStopwatch = findViewById(R.id.tabStopwatch);
        TextView tabTimer = findViewById(R.id.tabTimer);

        tabStopwatch.setOnClickListener(v -> {
            if (currentAppMode != AppMode.STOPWATCH) {
                if (timerManager.isRunning()) {
                    timerManager.pause();
                }
                currentAppMode = AppMode.STOPWATCH;
                tabStopwatch.setBackgroundResource(R.drawable.tab_bg_selected);
                tabStopwatch.setTextColor(getResources().getColor(R.color.text_primary));
                tabStopwatch.setTypeface(null, android.graphics.Typeface.BOLD);
                
                tabTimer.setBackground(null);
                tabTimer.setTextColor(getResources().getColor(R.color.text_secondary));
                tabTimer.setTypeface(null, android.graphics.Typeface.NORMAL);
                
                // Show the configuration button (label) for Stopwatch
                if (timerLabelLayout != null) {
                    timerLabelLayout.setVisibility(View.VISIBLE);
                    TextView timerLabel = findViewById(R.id.timerLabel);
                    if (timerLabel != null) timerLabel.setText("Stopwatch >");
                }
                
                btnStart.setImageResource(R.drawable.ic_play);
                btnReset.setVisibility(View.GONE);
                btnCategory.setVisibility(View.VISIBLE);
                btnStats.setVisibility(View.VISIBLE);
                
                updateUI(stopwatchManager.getElapsedTime(), false);
            }
        });

        tabTimer.setOnClickListener(v -> {
            if (currentAppMode != AppMode.TIMER) {
                if (stopwatchManager.isRunning()) {
                    stopwatchManager.pause();
                }
                currentAppMode = AppMode.TIMER;
                tabTimer.setBackgroundResource(R.drawable.tab_bg_selected);
                tabTimer.setTextColor(getResources().getColor(R.color.text_primary));
                tabTimer.setTypeface(null, android.graphics.Typeface.BOLD);
                
                tabStopwatch.setBackground(null);
                tabStopwatch.setTextColor(getResources().getColor(R.color.text_secondary));
                tabStopwatch.setTypeface(null, android.graphics.Typeface.NORMAL);
                
                if (timerLabelLayout != null) {
                    timerLabelLayout.setVisibility(View.VISIBLE);
                    TextView timerLabel = findViewById(R.id.timerLabel);
                    if (timerLabel != null) timerLabel.setText("Timer >");
                }
                
                btnStart.setImageResource(R.drawable.ic_play);
                btnReset.setVisibility(timerManager.getTimeLeft() != defaultTime ? View.VISIBLE : View.GONE);
                btnCategory.setVisibility(timerManager.getTimeLeft() != defaultTime ? View.GONE : View.VISIBLE);
                btnStats.setVisibility(timerManager.getTimeLeft() != defaultTime ? View.INVISIBLE : View.VISIBLE);
                
                updateUI(timerManager.getTimeLeft(), false);
            }
        });

        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        int minutes = result.getData().getIntExtra("focus_time", 25);
                        currentFocusDuration = minutes * 60 * 1000L;
                        defaultTime = currentFocusDuration;
                        timerManager.reset(currentFocusDuration);
                        if (currentAppMode == AppMode.TIMER) {
                            updateUI(currentFocusDuration, false);
                        }
                    }
                });

        btnStart.setOnClickListener(v -> {
            if (currentAppMode == AppMode.TIMER) {
                if (timerManager.isRunning()) {
                    timerManager.pause();
                    disableDND();
                    btnStart.setImageResource(R.drawable.ic_play);
                } else {
                    if (currentMode == Mode.FOCUS) {
                        sessionStartTimeMs = System.currentTimeMillis();
                        enableDND();
                    }
                    
                    btnStart.setImageResource(R.drawable.ic_pause);
                    btnCategory.setVisibility(View.GONE);
                    btnReset.setVisibility(View.VISIBLE);
                    btnStats.setVisibility(View.INVISIBLE);
                    
                    timerManager.start(new TimerManager.TimerListener() {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            updateUI(millisUntilFinished, true);
                        }

                        @Override
                        public void onFinish() {
                            handleTimerFinish();
                        }
                    });
                }
            } else {
                // Stopwatch Mode
                if (stopwatchManager.isRunning()) {
                    stopwatchManager.pause();
                    disableDND();
                    btnStart.setImageResource(R.drawable.ic_play);
                } else {
                    enableDND();
                    btnStart.setImageResource(R.drawable.ic_pause);
                    btnCategory.setVisibility(View.GONE);
                    btnReset.setVisibility(View.VISIBLE);
                    btnStats.setVisibility(View.INVISIBLE);
                    
                    stopwatchManager.start(elapsedMillis -> updateUI(elapsedMillis, true));
                }
            }
        });

        btnCategory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            launcher.launch(intent);
        });

        btnStats.setOnClickListener(v -> {
            Intent intent = new Intent(       MainActivity.this, StatsActivity.class);
            startActivity(intent);
        });

        setupClip(findViewById(R.id.flMinutesTopNextContainer), true);
        setupClip(findViewById(R.id.flMinutesBottomContainer), false);
        setupClip(findViewById(R.id.flMinutesFlip), true);

        setupClip(findViewById(R.id.flSecondsTopNextContainer), true);
        setupClip(findViewById(R.id.flSecondsBottomContainer), false);
        setupClip(findViewById(R.id.flSecondsFlip), true);

        if (currentAppMode == AppMode.TIMER) {
            updateUI(defaultTime, false);
        } else {
            updateUI(0, false);
        }
    }

    private void applyStoredSettings() {
        LocaleHelper.onAttach(this);
        android.content.SharedPreferences settingsPrefs = getSharedPreferences("Settings", MODE_PRIVATE);
        
        // Apply Theme
        String theme = settingsPrefs.getString("clock_theme", "dark");
        if (theme.equals("light")) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        }

        // Apply Language
        String langCode = settingsPrefs.getString("selected_language", "en");
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        android.content.res.Resources res = getResources();
        android.content.res.Configuration config = res.getConfiguration();
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    @Override
    protected void onResume() {
        super.onResume();
        isAppInForeground = true;
        if (isAlarmRinging && (alarmDialog == null || !alarmDialog.isShowing())) {
            showTimerCompleteDialog();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isAppInForeground = false;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Focus Alarm";
            String description = "Channel for focus mode completion alarms";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            // Set sound to null because our MediaPlayer handles the sound
            channel.setSound(null, null);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void handleTimerFinish() {
        btnStart.setImageResource(R.drawable.ic_play);
        
        if (currentMode == Mode.FOCUS) {
            long sessionEndTimeMs = System.currentTimeMillis();
            long actualDuration = currentFocusDuration; // For timer, we assume they completed the full duration
            dbHelper.addSession(sessionEndTimeMs - actualDuration, sessionEndTimeMs, actualDuration);
            remainingLoops--;
            
            if (remainingLoops > 0) {
                Toast.makeText(MainActivity.this, getString(R.string.focus_time) + " Complete! " + remainingLoops + " loops left. 🔥", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "All " + getString(R.string.focus_time) + " Sessions Complete! 🔥", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, getString(R.string.break_time) + " Complete! ☕", Toast.LENGTH_SHORT).show();
        }
        
        updateUI(0, false);
        playAlarmSound();
        
        showTimerCompleteDialog();
        
        btnReset.setVisibility(View.GONE);
        btnCategory.setVisibility(View.VISIBLE);
        btnStats.setVisibility(View.VISIBLE);
    }

    private void playAlarmSound() {
        android.content.SharedPreferences settingsPrefs = getSharedPreferences("Settings", MODE_PRIVATE);
        if (settingsPrefs.getBoolean("pref_dnd_mode", false)) {
            isAlarmRinging = true;
            return;
        }
        if (selectedSoundUri == null) {
            selectedSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM);
        }

        isAlarmRinging = true;

        try {
            currentMediaPlayer = new android.media.MediaPlayer();
            currentMediaPlayer.setDataSource(this, selectedSoundUri);
            
            android.media.AudioAttributes attributes = new android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            
            currentMediaPlayer.setAudioAttributes(attributes);
            currentMediaPlayer.setLooping(true);
            currentMediaPlayer.prepare();
            currentMediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to RingtoneManager if MediaPlayer fails
            try {
                currentRingtone = android.media.RingtoneManager.getRingtone(this, selectedSoundUri);
                if (currentRingtone != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        currentRingtone.setLooping(true);
                    }
                    currentRingtone.play();
                } else {
                    // Last resort: default alarm
                    currentRingtone = android.media.RingtoneManager.getRingtone(this, 
                            android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM));
                    if (currentRingtone != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            currentRingtone.setLooping(true);
                        }
                        currentRingtone.play();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void stopAlarmSound() {
        isAlarmRinging = false;
        disableDND();
        
        if (currentMediaPlayer != null) {
            try {
                if (currentMediaPlayer.isPlaying()) {
                    currentMediaPlayer.stop();
                }
                currentMediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            currentMediaPlayer = null;
        }
        
        if (currentRingtone != null) {
            try {
                if (currentRingtone.isPlaying()) {
                    currentRingtone.stop();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            currentRingtone = null;
        }

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    private void showTimerCompleteDialog() {
        if (alarmDialog != null && alarmDialog.isShowing()) {
            alarmDialog.dismiss();
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_timer_complete, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        com.google.android.material.button.MaterialButton btnNext = dialogView.findViewById(R.id.btnDialogNext);
        com.google.android.material.button.MaterialButton btnStop = dialogView.findViewById(R.id.btnDialogStop);

        if (currentMode == Mode.FOCUS) {
            int completedRounds = totalLoops - remainingLoops;
            if (remainingLoops > 0) {
                tvTitle.setText("Focus Round Complete 🎯");
                tvMessage.setText("Round " + completedRounds + " of " + totalLoops + " complete. Continue?");
                btnNext.setText("Next");
                
                btnNext.setOnClickListener(v -> {
                    stopAlarmSound();
                    alarmDialog.dismiss();
                    if (breakDurationMs > 0) {
                        startBreak();
                    } else {
                        startFocus();
                    }
                });
            } else {
                tvTitle.setText("Session Complete 🏆");
                tvMessage.setText("You finished all " + totalLoops + " rounds.");
                btnNext.setText("Restart");
                btnStop.setText("Done");

                btnNext.setOnClickListener(v -> {
                    stopAlarmSound();
                    alarmDialog.dismiss();
                    remainingLoops = totalLoops;
                    startFocus();
                });
                
                btnStop.setOnClickListener(v -> {
                    stopAlarmSound();
                    alarmDialog.dismiss();
                    resetSession();
                });
                // Skip the default stop handler below for this specific case
            }
        } else {
            tvTitle.setText("Break Finished ☕");
            tvMessage.setText("Ready to start Round " + (totalLoops - remainingLoops + 1) + "?");
            btnNext.setText("Next");

            btnNext.setOnClickListener(v -> {
                stopAlarmSound();
                alarmDialog.dismiss();
                startFocus();
            });
        }

        btnStop.setOnClickListener(v -> {
            stopAlarmSound();
            alarmDialog.dismiss();
            resetSession();
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);

        alarmDialog = builder.create();
        if (alarmDialog.getWindow() != null) {
            alarmDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            
            android.view.WindowManager.LayoutParams params = alarmDialog.getWindow().getAttributes();
            params.gravity = android.view.Gravity.TOP;
            params.y = (int) (50 * getResources().getDisplayMetrics().density);
            alarmDialog.getWindow().setAttributes(params);
        }
        
        if (isAppInForeground) {
            alarmDialog.show();
        } else {
            showAlarmNotification();
        }
    }

    private void resetSession() {
        disableDND();
        remainingLoops = totalLoops;
        currentMode = Mode.FOCUS;
        timerManager.reset(currentFocusDuration);
        updateUI(currentFocusDuration, false);
        btnStart.setImageResource(R.drawable.ic_play);
        btnReset.setVisibility(View.GONE);
        btnCategory.setVisibility(View.VISIBLE);
        btnStats.setVisibility(View.VISIBLE);
    }

    private void startBreak() {
        currentMode = Mode.BREAK;
        timerManager.reset(breakDurationMs);
        updateUI(breakDurationMs, false);
        
        startTimerInternal();
    }

    private void startFocus() {
        currentMode = Mode.FOCUS;
        timerManager.reset(currentFocusDuration);
        updateUI(currentFocusDuration, false);
        
        startTimerInternal();
    }

    private void enableDND() {
        android.content.SharedPreferences settingsPrefs = getSharedPreferences("Settings", MODE_PRIVATE);
        boolean dndToggle = settingsPrefs.getBoolean("pref_dnd_mode", false);

        if (dndToggle) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                if (notificationManager.isNotificationPolicyAccessGranted()) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
                    isDndEnabledByApp = true;
                } else {
                    // If toggle is ON but permission was revoked, ask for it
                    Toast.makeText(this, "Please grant DND permission to silence notifications", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                    startActivity(intent);
                    // Also turn off the toggle so they have to re-enable it after granting
                    settingsPrefs.edit().putBoolean("pref_dnd_mode", false).apply();
                }
            }
        }
    }

    private void disableDND() {
        if (isDndEnabledByApp) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null && notificationManager.isNotificationPolicyAccessGranted()) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
            }
            isDndEnabledByApp = false;
        }
    }

    private void startTimerInternal() {
        if (currentMode == Mode.FOCUS) {
            sessionStartTimeMs = System.currentTimeMillis();
            enableDND();
        }
        
        btnStart.setImageResource(R.drawable.ic_pause);
        btnCategory.setVisibility(View.GONE);
        btnReset.setVisibility(View.VISIBLE);
        btnStats.setVisibility(View.INVISIBLE);
        
        timerManager.start(new TimerManager.TimerListener() {
            @Override
            public void onTick(long millisUntilFinished) {
                updateUI(millisUntilFinished, true);
            }

            @Override
            public void onFinish() {
                handleTimerFinish();
            }
        });
    }

    private void showAlarmNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags);

        String title = currentMode == Mode.FOCUS ? "Focus Complete" : "Break Finished";
        String message = currentMode == Mode.FOCUS ? "Time for a break!" : "Ready to focus?";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_play) 
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setOngoing(true);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private void setupClip(final View container, final boolean top) {
        View parent = (View) container.getParent();
        int fullHeight = parent.getHeight();
        if (fullHeight <= 0) {
            container.post(() -> setupClip(container, top));
            return;
        }

        View text = ((android.view.ViewGroup) container).getChildAt(0);
        if (text != null) {
            android.view.ViewGroup.LayoutParams lp = text.getLayoutParams();
            lp.height = fullHeight;
            text.setLayoutParams(lp);
        }

        if (top) {
            container.getLayoutParams().height = fullHeight / 2;
            if (text != null) {
                text.setTranslationY(0);
            }
        } else {
            container.getLayoutParams().height = fullHeight / 2;
            container.setTranslationY(fullHeight / 2f);
            if (text != null) {
                text.setTranslationY(-fullHeight / 2f);
            }
        }
        container.requestLayout();
    }

    private void updateUI(long millis, boolean animate) {
        int min = (int) (millis / 1000) / 60;
        int sec = (int) (millis / 1000) % 60;

        String minStr = String.format(Locale.US, "%02d", min);
        String secStr = String.format(Locale.US, "%02d", sec);

        int centi = (int) (millis % 1000) / 10;
        String msStr = String.format(Locale.US, "%02d", centi);

        if (!minStr.equals(lastMinute)) {
            if (animate) {
                applyFlipAnimation(flMinutesFlip, tvMinutesFlip, tvMinutesTopNext, tvMinutesBottom, vMinutesShadow, minStr, lastMinute);
            } else {
                tvMinutesBottom.setText(minStr);
                tvMinutesFlip.setText(minStr);
                tvMinutesTopNext.setText(minStr);
            }
            lastMinute = minStr;
        }

        if (!secStr.equals(lastSecond)) {
            if (animate) {
                applyFlipAnimation(flSecondsFlip, tvSecondsFlip, tvSecondsTopNext, tvSecondsBottom, vSecondsShadow, secStr, lastSecond);
            } else {
                tvSecondsBottom.setText(secStr);
                tvSecondsFlip.setText(secStr);
                tvSecondsTopNext.setText(secStr);
            }
            lastSecond = secStr;
        }

        tvMilliseconds.setText(msStr);
    }

    private void applyFlipAnimation(final View flapContainer, final TextView flapText,
                                    final TextView topNext, final TextView bottomCurrent,
                                    final View shadow, final String newText, final String oldText) {

        final int fullHeight = ((View) flapContainer.getParent()).getHeight();
        if (fullHeight <= 0) return; // Wait for layout

        final float halfHeight = fullHeight / 2f;

        topNext.setText(newText);
        bottomCurrent.setText(oldText);

        // Setup flap text layout properly
        android.view.ViewGroup.LayoutParams lp = flapText.getLayoutParams();
        lp.height = fullHeight;
        flapText.setLayoutParams(lp);

        float scale = getResources().getDisplayMetrics().density;
        flapContainer.setCameraDistance(8000 * scale);

        // Falling flip animation (Same for Timer and Stopwatch)
        flapText.setText(oldText);
        flapText.setTranslationY(0);
        flapContainer.setTranslationY(0);
        flapContainer.getLayoutParams().height = (int) halfHeight;
        flapContainer.setPivotY(halfHeight);
        flapContainer.setRotationX(0f);
        flapContainer.setVisibility(View.VISIBLE);
        setupClip(flapContainer, true);
        shadow.setAlpha(0f);

        flapContainer.animate()
                .rotationX(-90f)
                .setDuration(150)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    flapText.setText(newText);
                    flapText.setTranslationY(-halfHeight);
                    flapContainer.setTranslationY(halfHeight);
                    flapContainer.setPivotY(0);
                    flapContainer.setRotationX(90f);
                    setupClip(flapContainer, false);

                    flapContainer.animate()
                            .rotationX(0f)
                            .setDuration(150)
                            .setInterpolator(new DecelerateInterpolator())
                            .withEndAction(() -> {
                                bottomCurrent.setText(newText);
                                flapContainer.setVisibility(View.INVISIBLE);
                            }).start();
                    shadow.animate().alpha(0f).setDuration(150).start();
                }).start();
        shadow.animate().alpha(1.0f).setDuration(150).start();
    }

    private void applyBrightness() {
        android.content.SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        float brightness = prefs.getFloat("pref_brightness", -1f);
        if (brightness != -1f) {
            android.view.WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.screenBrightness = brightness;
            getWindow().setAttributes(layoutParams);
        }
    }
}
