package com.upgradefinance.utils;

import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;

public class AppLogger {
    private static final List<String> logs = new ArrayList<>();
    private static LogListener listener;

    public interface LogListener {
        void onLogAdded();
    }

    public static synchronized void log(String message) {
        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
        String logLine = "[" + timestamp + "] " + message;
        logs.add(0, logLine); // Add to top
        if (logs.size() > 50) {
            logs.remove(logs.size() - 1);
        }
        if (listener != null) {
            new Handler(Looper.getMainLooper()).post(() -> listener.onLogAdded());
        }
    }

    public static synchronized List<String> getLogs() {
        return new ArrayList<>(logs);
    }

    public static void setListener(LogListener l) {
        listener = l;
    }
}
