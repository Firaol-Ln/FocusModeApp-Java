package com.example.focusmodejv.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.focusmodejv.R;
import com.example.focusmodejv.timer.TimerManager;
import com.example.focusmodejv.ui.CategoryActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvClock, tvSession;
    private Button btnStart, btnPause, btnReset, btnCategory;

    private TimerManager timerManager;

    private long defaultTime = 25 * 60 * 1000;
    private int sessionCount = 0;

    private ActivityResultLauncher<Intent> launcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvClock = findViewById(R.id.tvClock);
        tvSession = findViewById(R.id.tvSessionCounter);
        btnStart = findViewById(R.id.btnStart);
        btnPause = findViewById(R.id.btnPause);
        btnReset = findViewById(R.id.btnReset);
        btnCategory = findViewById(R.id.btnCategories);

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
                        tvSession.setText("Sessions: " + sessionCount);
                        Toast.makeText(MainActivity.this, "Done 🔥", Toast.LENGTH_SHORT).show();
                    }
                })
        );

        btnPause.setOnClickListener(v -> timerManager.pause());

        btnReset.setOnClickListener(v -> {
            timerManager.reset(defaultTime);
            updateTime(defaultTime);
        });

        btnCategory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CategoryActivity.class);
            launcher.launch(intent);
        });

        updateTime(defaultTime);
    }

    private void updateTime(long millis) {
        int min = (int) millis / 1000 / 60;
        int sec = (int) millis / 1000 % 60;

        String time = String.format(Locale.getDefault(), "%02d:%02d", min, sec);

        tvClock.setText(time);
    }
}