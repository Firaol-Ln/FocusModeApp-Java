package com.example.focusmodejv;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.focusmodejv.R;
import com.google.android.material.card.MaterialCardView;

public class CategoryActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        MaterialCardView cardStudy = findViewById(R.id.cardStudy);
        MaterialCardView cardReading = findViewById(R.id.cardReading);
        MaterialCardView cardCoding = findViewById(R.id.cardCoding);
        MaterialCardView cardSport = findViewById(R.id.cardSport);

        if (cardStudy != null) {
            cardStudy.setOnClickListener(v -> finishWithResult(45));
        }
        if (cardReading != null) {
            cardReading.setOnClickListener(v -> finishWithResult(30));
        }
        if (cardCoding != null) {
            cardCoding.setOnClickListener(v -> finishWithResult(60));
        }
        if (cardSport != null) {
            cardSport.setOnClickListener(v -> finishWithResult(20));
        }
    }

    private void finishWithResult(int focusTime) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("focus_time", focusTime);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
