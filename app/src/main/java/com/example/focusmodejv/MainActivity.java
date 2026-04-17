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
import com.example.focusmodejv.timer.TimerBottomSheet;
import com.example.focusmodejv.timer.TimerManager;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvMinutesTopNext, tvMinutesBottom, tvMinutesFlip;
    private TextView tvSecondsTopNext, tvSecondsBottom, tvSecondsFlip;
    private View flMinutesFlip, flSecondsFlip, vMinutesShadow, vSecondsShadow;
    private TextView tvMilliseconds;
    private ImageButton btnStart, btnCategory, btnStats, btnReset;

    private TimerManager timerManager;
    private DatabaseHelper dbHelper;

    private long currentFocusDuration = 25 * 60 * 1000L;
    private long defaultTime = 25 * 60 * 1000L;
    
    private String lastSecond = "00";
    private String lastMinute = "25";

    private android.net.Uri selectedSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);

    private ActivityResultLauncher<Intent> launcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        
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

        android.widget.LinearLayout timerLabelLayout = findViewById(R.id.timerLabelLayout);
        if (timerLabelLayout != null) {
            timerLabelLayout.setOnClickListener(v -> {
                TimerBottomSheet bottomSheet = new TimerBottomSheet();
                bottomSheet.setListener((hours, minutes, seconds, color, tag, soundUri) -> {
                    long newDuration = (hours * 3600L + minutes * 60L + seconds) * 1000L;
                    if (newDuration > 0) {
                        currentFocusDuration = newDuration;
                        defaultTime = currentFocusDuration;
                        timerManager.reset(currentFocusDuration);
                        updateUI(currentFocusDuration, false);
                    }
                    View indicator = findViewById(R.id.timerIndicator);
                    if (indicator != null) indicator.setBackgroundColor(color);
                    TextView label = findViewById(R.id.timerLabel);
                    if (label != null) label.setText(tag + " >");
                    
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
        btnReset.setColorFilter(android.graphics.Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        btnReset.setPadding(padding, padding, padding, padding);
        btnReset.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        btnReset.setVisibility(View.GONE);
        btnReset.setContentDescription("Reset");

        android.widget.LinearLayout bottomControls = findViewById(R.id.bottomControls);
        bottomControls.addView(btnReset, 0);

        btnReset.setOnClickListener(v -> {
            timerManager.reset(currentFocusDuration);
            updateUI(currentFocusDuration, false);
            btnStart.setImageResource(R.drawable.ic_play);
            btnReset.setVisibility(View.GONE);
            btnCategory.setVisibility(View.VISIBLE);
            btnStats.setVisibility(View.VISIBLE);
        });

        timerManager = new TimerManager(defaultTime);

        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        int minutes = result.getData().getIntExtra("focus_time", 25);
                        currentFocusDuration = minutes * 60 * 1000L;
                        defaultTime = currentFocusDuration;
                        timerManager.reset(currentFocusDuration);
                        updateUI(currentFocusDuration, false);
                    }
                });

        btnStart.setOnClickListener(v -> {
            if (timerManager.isRunning()) {
                timerManager.pause();
                btnStart.setImageResource(R.drawable.ic_play);
            } else {
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
                        btnStart.setImageResource(R.drawable.ic_play);
                        dbHelper.addSession(currentFocusDuration, System.currentTimeMillis());
                        Toast.makeText(MainActivity.this, "Focus Session Complete! 🔥", Toast.LENGTH_SHORT).show();
                        updateUI(0, false);
                        
                        if (selectedSoundUri != null) {
                            android.media.Ringtone r = android.media.RingtoneManager.getRingtone(getApplicationContext(), selectedSoundUri);
                            if (r != null) r.play();
                        }
                        
                        btnReset.setVisibility(View.GONE);
                        btnCategory.setVisibility(View.VISIBLE);
                        btnStats.setVisibility(View.VISIBLE);
                    }
                });
            }
        });

        btnCategory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CategoryActivity.class);
            launcher.launch(intent);
        });

        btnStats.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StatsActivity.class);
            startActivity(intent);
        });

        setupClip(findViewById(R.id.flMinutesTopNextContainer), true);
        setupClip(findViewById(R.id.flMinutesBottomContainer), false);
        setupClip(findViewById(R.id.flMinutesFlip), true);

        setupClip(findViewById(R.id.flSecondsTopNextContainer), true);
        setupClip(findViewById(R.id.flSecondsBottomContainer), false);
        setupClip(findViewById(R.id.flSecondsFlip), true);

        updateUI(defaultTime, false);
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
        
        // Start state: Flap is at the top, showing old text's top half
        flapText.setText(oldText);
        android.view.ViewGroup.LayoutParams lp = flapText.getLayoutParams();
        lp.height = fullHeight;
        flapText.setLayoutParams(lp);
        flapText.setTranslationY(0);

        flapContainer.getLayoutParams().height = (int) halfHeight;
        flapContainer.setTranslationY(0);
        flapContainer.setPivotX(flapContainer.getWidth() / 2f);
        flapContainer.setPivotY(halfHeight); // Hinge is at the bottom of the top half
        flapContainer.setRotationX(0f);
        flapContainer.setVisibility(View.VISIBLE);
        
        setupClip(flapContainer, true); // Clip to top half
        shadow.setAlpha(0f);

        float scale = getResources().getDisplayMetrics().density;
        flapContainer.setCameraDistance(8000 * scale);

        // Stage 1: Flip top half down to middle
        flapContainer.animate()
                .rotationX(-90f)
                .setDuration(150)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(() -> {
                    // Middle state: Switch flap to bottom half and new text
                    flapText.setText(newText);
                    flapText.setTranslationY(-halfHeight);
                    
                    flapContainer.setTranslationY(halfHeight);
                    flapContainer.setPivotY(0); // Hinge is now at the top of the bottom half
                    flapContainer.setRotationX(90f); // Corresponding angle for the new pivot
                    
                    setupClip(flapContainer, false); // Clip to bottom half
                    shadow.setAlpha(1f);

                    // Stage 2: Flip from middle down to bottom
                    flapContainer.animate()
                            .rotationX(0f)
                            .setDuration(150)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .withEndAction(() -> {
                                bottomCurrent.setText(newText);
                                flapContainer.setVisibility(View.INVISIBLE);
                            })
                            .start();
                    
                    shadow.animate().alpha(0f).setDuration(150).start();
                })
                .start();
        
        shadow.animate().alpha(1.0f).setDuration(150).start();
    }
}
