package com.coretec.sensing.view;


import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import java.text.DecimalFormat;

public class MillisecondChronometer extends AppCompatTextView {
    @SuppressWarnings("unused")
    private static final String TAG = "Chronometer";

    public interface OnChronometerTickListener {
        void onChronometerTick(MillisecondChronometer chronometer);
    }

    private long base;
    private boolean isVisible;
    private boolean isStarted;
    private boolean isRunning;
    private OnChronometerTickListener onChronometerTickListener;

    private static final int TICK_WHAT = 2;

    private long timeElapsed;

    public MillisecondChronometer(Context context) {
        this (context, null, 0);
    }

    public MillisecondChronometer(Context context, AttributeSet attrs) {
        this (context, attrs, 0);
    }

    public MillisecondChronometer(Context context, AttributeSet attrs, int defStyle) {
        super (context, attrs, defStyle);

        init();
    }

    private void init() {
        base = SystemClock.elapsedRealtime();
        updateText(base);
    }

    public void setBase(long base) {
        this.base = base;
        dispatchChronometerTick();
        updateText(SystemClock.elapsedRealtime());
    }

    public long getBase() {
        return base;
    }

    public void setOnChronometerTickListener(
            OnChronometerTickListener listener) {
        onChronometerTickListener = listener;
    }

    public OnChronometerTickListener getOnChronometerTickListener() {
        return onChronometerTickListener;
    }

    public void start() {
        isStarted = true;
        updateRunning();
    }

    public void stop() {
        isStarted = false;
        updateRunning();
    }


    public void setStarted(boolean started) {
        isStarted = started;
        updateRunning();
    }

    @Override
    protected void onDetachedFromWindow() {
        super .onDetachedFromWindow();
        isVisible = false;
        updateRunning();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super .onWindowVisibilityChanged(visibility);
        isVisible = visibility == VISIBLE;
        updateRunning();
    }

    private synchronized void updateText(long now) {
        timeElapsed = now - base;

        DecimalFormat df = new DecimalFormat("00");

        int hours = (int)(timeElapsed / (3600 * 1000));
        int remaining = (int)(timeElapsed % (3600 * 1000));

        int minutes = (int)(remaining / (60 * 1000));
        remaining = (int)(remaining % (60 * 1000));

        int seconds = (int)(remaining / 1000);
        remaining = (int)(remaining % (1000));

        int milliseconds = (int)(((int)timeElapsed % 1000) / 100);

        String text = "";

        if (hours > 0) {
            text += df.format(hours) + ":";
        }

        text += df.format(minutes) + ":";
        text += df.format(seconds) + ":";
        text += Integer.toString(milliseconds);

        setText(text);
    }

    private void updateRunning() {
        boolean running = isVisible && isStarted;
        if (running != isRunning) {
            if (running) {
                updateText(SystemClock.elapsedRealtime());
                dispatchChronometerTick();
                mHandler.sendMessageDelayed(Message.obtain(mHandler,
                        TICK_WHAT), 100);
            } else {
                mHandler.removeMessages(TICK_WHAT);
            }
            isRunning = running;
        }
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message m) {
            if (isRunning) {
                updateText(SystemClock.elapsedRealtime());
                dispatchChronometerTick();
                sendMessageDelayed(Message.obtain(this , TICK_WHAT),
                        100);
            }
        }
    };

    void dispatchChronometerTick() {
        if (onChronometerTickListener != null) {
            onChronometerTickListener.onChronometerTick(this);
        }
    }

    public long getTimeElapsed() {
        return timeElapsed;
    }
}
