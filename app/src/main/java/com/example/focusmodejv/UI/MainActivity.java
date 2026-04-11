package com.example.focusmodejv.UI;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.focusmodejv.R;
import com.example.focusmodejv.timer.TimerManager;
import com.example.focusmodejv.UI.CategoryActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvMinutes, tvSeconds, tvSession;
    private ImageButton btnStart, btnCategory, btnStats;

    private TimerManager timerManager;

    private long defaultTime = 25 * 60 * 1000;
    private int sessionCount = 0;

    private ActivityResultLauncher<Intent> launcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvMinutes = findViewById(R.id.tvMinutes);
        tvSeconds = findViewById(R.id.tvSeconds);
        btnStart = findViewById(R.id.btnStart);
        btnCategory = findViewById(R.id.btnCategories);
        btnStats = findViewById(R.id.btnStats);

        timerManager = new TimerManager(defaultTime);

        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        int minutes = result.getData().getIntExtra("focus_time", 25);
                        defaultTime = minutes * 60 * 1000L;
                        timerManager.reset(defaultTime);
                        updateTime(defaultTime);
                    }
                });

        btnStart.setOnClickListener(v ->
                timerManager.start(new TimerManager.TimerListener() {
                    @Override
                    public void onTick(long millis) {
                        updateTime(millis);
                    }

                    @Override
                    public void onFinish() {
                        sessionCount++;
                        // tvSession.setText("Sessions: " + sessionCount);
                        Toast.makeText(MainActivity.this, "Done 🔥", Toast.LENGTH_SHORT).show();
                    }
                })
        );

        btnCategory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CategoryActivity.class);
            launcher.launch(intent);
        });

        updateTime(defaultTime);
    }

    private void updateTime(long millis) {
        int min = (int) millis / 1000 / 60;
        int sec = (int) millis / 1000 % 60;

        if (tvMinutes != null) tvMinutes.setText(String.format(Locale.getDefault(), "%02d", min));
        if (tvSeconds != null) tvSeconds.setText(String.format(Locale.getDefault(), "%02d", sec));
    }
}