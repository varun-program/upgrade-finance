package com.upgradefinance;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.gson.Gson;
import com.upgradefinance.db.AppDatabase;
import com.upgradefinance.receiver.SMSReceiver;
import com.upgradefinance.worker.SyncWorker;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int SMS_PERMISSION_CODE = 101;
    private static final String PREFS_NAME = "UpgradeFinancePrefs";

    private EditText etServerUrl, etEmail, etPassword;
    private Button btnConnect, btnDisconnect, btnGrantSms, btnGrantNotifications, btnSyncNow, btnSimulateSms;
    private TextView tvSmsPermissionStatus, tvNotificationStatus, tvLastSync, tvTxCount, tvUserEmail, tvServerStatus;
    private View cardLogin, cardStatus;

    private SharedPreferences prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind layout views
        etServerUrl = findViewById(R.id.etServerUrl);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnConnect = findViewById(R.id.btnConnect);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        btnGrantSms = findViewById(R.id.btnGrantSms);
        btnGrantNotifications = findViewById(R.id.btnGrantNotifications);
        btnSyncNow = findViewById(R.id.btnSyncNow);
        btnSimulateSms = findViewById(R.id.btnSimulateSms);
        tvSmsPermissionStatus = findViewById(R.id.tvSmsPermissionStatus);
        tvNotificationStatus = findViewById(R.id.tvNotificationStatus);
        tvLastSync = findViewById(R.id.tvLastSync);
        tvTxCount = findViewById(R.id.tvTxCount);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvServerStatus = findViewById(R.id.tvServerStatus);
        cardLogin = findViewById(R.id.cardLogin);
        cardStatus = findViewById(R.id.cardStatus);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        setupAuthUI();
        setupListeners();
        updatePermissionsStatus();
        updateSyncStats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionsStatus();
        updateSyncStats();
    }

    private void setupAuthUI() {
        String token = prefs.getString("auth_token", null);
        String email = prefs.getString("auth_email", null);
        String server = prefs.getString("auth_server", null);

        if (token != null) {
            cardLogin.setVisibility(View.GONE);
            cardStatus.setVisibility(View.VISIBLE);
            tvUserEmail.setText("Connected as: " + email);
            tvServerStatus.setText("Sync Server: " + server);
        } else {
            cardLogin.setVisibility(View.VISIBLE);
            cardStatus.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        btnConnect.setOnClickListener(v -> handleConnect());
        btnDisconnect.setOnClickListener(v -> handleDisconnect());
        btnGrantSms.setOnClickListener(v -> requestSmsPermission());
        btnGrantNotifications.setOnClickListener(v -> openNotificationAccessSettings());
        btnSyncNow.setOnClickListener(v -> triggerSyncNow());
        btnSimulateSms.setOnClickListener(v -> handleSimulateSms());
    }

    private void handleConnect() {
        final String serverUrl = etServerUrl.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (serverUrl.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConnect.setEnabled(false);
        btnConnect.setText("Connecting...");

        executor.execute(() -> {
            try {
                URL url = new URL(serverUrl + "/api/auth/login");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                Map<String, String> payload = new HashMap<>();
                payload.put("email", email);
                payload.put("password", password);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(new Gson().toJson(payload).getBytes());
                    os.flush();
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                    Map<String, Object> response = new Gson().fromJson(reader, Map.class);
                    String token = (String) response.get("token");

                    // Save credentials in Prefs
                    prefs.edit()
                            .putString("auth_token", token)
                            .putString("auth_email", email)
                            .putString("auth_server", serverUrl)
                            .apply();

                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(MainActivity.this, "Successfully connected!", Toast.LENGTH_SHORT).show();
                        setupAuthUI();
                        triggerSyncNow();
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(MainActivity.this, "Authentication failed. Error code: " + code, Toast.LENGTH_LONG).show();
                        btnConnect.setEnabled(true);
                        btnConnect.setText("Connect & Sync");
                    });
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(MainActivity.this, "Connection failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnConnect.setEnabled(true);
                    btnConnect.setText("Connect & Sync");
                });
            }
        });
    }

    private void handleDisconnect() {
        prefs.edit()
                .remove("auth_token")
                .remove("auth_email")
                .remove("auth_server")
                .remove("last_sync_timestamp")
                .apply();
        setupAuthUI();
        updateSyncStats();
        Toast.makeText(this, "Disconnected successfully.", Toast.LENGTH_SHORT).show();
        btnConnect.setEnabled(true);
        btnConnect.setText("Connect & Sync");
    }

    private void updatePermissionsStatus() {
        // SMS permission check
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
            tvSmsPermissionStatus.setText("GRANTED");
            tvSmsPermissionStatus.setTextColor(0xFF10B981); // Green
            btnGrantSms.setVisibility(View.GONE);
        } else {
            tvSmsPermissionStatus.setText("DENIED");
            tvSmsPermissionStatus.setTextColor(0xFFEF4444); // Red
            btnGrantSms.setVisibility(View.VISIBLE);
        }

        // Notification Listener check
        if (isNotificationServiceEnabled()) {
            tvNotificationStatus.setText("ENABLED");
            tvNotificationStatus.setTextColor(0xFF10B981); // Green
            btnGrantNotifications.setVisibility(View.GONE);
        } else {
            tvNotificationStatus.setText("DISABLED");
            tvNotificationStatus.setTextColor(0xFFEF4444); // Red
            btnGrantNotifications.setVisibility(View.VISIBLE);
        }
    }

    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (flat != null) {
            final String[] names = flat.split(":");
            for (String name : names) {
                if (name.contains(pkgName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS}, SMS_PERMISSION_CODE);
    }

    private void openNotificationAccessSettings() {
        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        startActivity(intent);
    }

    private void triggerSyncNow() {
        if (prefs.getString("auth_token", null) == null) {
            Toast.makeText(this, "Please connect your account first", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Triggering sync...", Toast.LENGTH_SHORT).show();
        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class).build();
        WorkManager.getInstance(this).enqueue(syncRequest);

        // Schedule check-in to refresh numbers
        new Handler(Looper.getMainLooper()).postDelayed(this::updateSyncStats, 3000);
    }

    private void updateSyncStats() {
        long lastSync = prefs.getLong("last_sync_timestamp", 0);
        if (lastSync == 0) {
            tvLastSync.setText("Last Synced: Never");
        } else {
            java.util.Date date = new java.util.Date(lastSync);
            tvLastSync.setText("Last Synced: " + date.toString());
        }

        executor.execute(() -> {
            try {
                int count = AppDatabase.getDatabase(MainActivity.this).transactionDao().getChangesSince(0).size();
                new Handler(Looper.getMainLooper()).post(() -> tvTxCount.setText("Transactions Cached: " + count));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            updatePermissionsStatus();
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS Permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS Permission denied. Tracking will not work.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void handleSimulateSms() {
        Toast.makeText(this, "Simulating incoming payment SMS...", Toast.LENGTH_SHORT).show();
        executor.execute(() -> {
            try {
                // Simulate Kotak SMS body
                String smsBody = "Sent Rs.14.00 from XXXXXX7215 to Swiggy. Ref ID: 678901234567";
                new SMSReceiver().parseAndSaveTransaction(MainActivity.this, smsBody, "JM-KOTAKD-S");
                
                // Wait briefly and update stats
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Toast.makeText(MainActivity.this, "Simulation completed!", Toast.LENGTH_SHORT).show();
                    updateSyncStats();
                }, 1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
