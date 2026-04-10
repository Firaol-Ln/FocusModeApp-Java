
package com.example.focusmodejv.timer;

import android.os.CountDownTimer;

public class TimerManager {

    private CountDownTimer countDownTimer;
    private long timeLeftInMillis;
    private boolean isRunning = false;

    public interface TimerListener {
        void onTick(long millisUntilFinished);
        void onFinish();
    }

    public TimerManager(long startTime) {
        this.timeLeftInMillis = startTime;
    }

    public void start(TimerListener listener) {
        if (isRunning) return;

        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                listener.onTick(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                isRunning = false;
                timeLeftInMillis = 0;
                listener.onFinish();
            }
        }.start();

        isRunning = true;
    }

    public void pause() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning = false;
    }

    public void reset(long newTime) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning = false;
        timeLeftInMillis = newTime;
    }

    public long getTimeLeft() {
        return timeLeftInMillis;
    }

    public boolean isRunning() {
        return isRunning;
    }
}
