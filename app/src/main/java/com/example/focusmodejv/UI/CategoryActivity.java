package com.example.focusmodejv.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.focusmodejv.R;

public class CategoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        Button btnWork = findViewById(R.id.btnWork);
        Button btnShortBreak = findViewById(R.id.btnShortBreak);
        Button btnLongBreak = findViewById(R.id.btnLongBreak);

        btnWork.setOnClickListener(v -> finishWithResult(25));
        btnShortBreak.setOnClickListener(v -> finishWithResult(5));
        btnLongBreak.setOnClickListener(v -> finishWithResult(15));
    }

    private void finishWithResult(int focusTime) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("focus_time", focusTime);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
