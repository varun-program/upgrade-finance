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
import com.upgradefinance.db.TransactionDao;
import com.upgradefinance.model.LocalTransaction;
import com.upgradefinance.receiver.SMSReceiver;
import com.upgradefinance.utils.AppLogger;
import com.upgradefinance.worker.SyncWorker;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private Button btnConnect, btnDisconnect, btnGrantSms, btnGrantNotifications, btnSyncNow, btnSimulateSms, btnScanInbox;
    private TextView tvSmsPermissionStatus, tvNotificationStatus, tvLastSync, tvTxCount, tvUserEmail, tvServerStatus, tvLogs;
    private View cardLogin, cardStatus;

    private SharedPreferences prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SMSReceiver dynamicSmsReceiver;

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
        btnScanInbox = findViewById(R.id.btnScanInbox);
        tvSmsPermissionStatus = findViewById(R.id.tvSmsPermissionStatus);
        tvNotificationStatus = findViewById(R.id.tvNotificationStatus);
        tvLastSync = findViewById(R.id.tvLastSync);
        tvTxCount = findViewById(R.id.tvTxCount);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvServerStatus = findViewById(R.id.tvServerStatus);
        cardLogin = findViewById(R.id.cardLogin);
        cardStatus = findViewById(R.id.cardStatus);
        tvLogs = findViewById(R.id.tvLogs);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        setupAuthUI();
        setupListeners();
        updatePermissionsStatus();
        updateSyncStats();

        AppLogger.setListener(() -> updateLogDisplay());
        updateLogDisplay();

        // Register SMS receiver dynamically as a backup
        dynamicSmsReceiver = new SMSReceiver();
        android.content.IntentFilter filter = new android.content.IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dynamicSmsReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(dynamicSmsReceiver, filter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionsStatus();
        updateSyncStats();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dynamicSmsReceiver != null) {
            try {
                unregisterReceiver(dynamicSmsReceiver);
            } catch (Exception e) {
                // ignore
            }
        }
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
        btnScanInbox.setOnClickListener(v -> handleScanInbox());
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
                int count = AppDatabase.getDatabase(MainActivity.this).transactionDao().getActiveCount();
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

    private void updateLogDisplay() {
        java.util.List<String> logLines = AppLogger.getLogs();
        if (logLines.isEmpty()) {
            tvLogs.setText("No diagnostic events logged yet.");
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (String line : logLines) {
            builder.append(line).append("\n");
        }
        tvLogs.setText(builder.toString());
    }

    private void handleScanInbox() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Please grant SMS permission first!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(this, "Scanning SMS inbox...", Toast.LENGTH_SHORT).show();
        AppLogger.log("SMS Scan: Started inbox scan...");
        
        executor.execute(() -> {
            int totalFound = 0;
            int newSaved = 0;
            android.database.Cursor cursor = null;
            try {
                android.net.Uri uri = android.net.Uri.parse("content://sms/inbox");
                // Get messages from the last 7 days to keep it fast
                long cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
                String selection = "date > ?";
                String[] selectionArgs = new String[]{String.valueOf(cutoffTime)};
                
                cursor = getContentResolver().query(uri, new String[]{"address", "body", "date"}, selection, selectionArgs, "date DESC");
                
                if (cursor != null && cursor.moveToFirst()) {
                    AppDatabase db = AppDatabase.getDatabase(MainActivity.this);
                    TransactionDao dao = db.transactionDao();
                    
                    int addressIndex = cursor.getColumnIndexOrThrow("address");
                    int bodyIndex = cursor.getColumnIndexOrThrow("body");
                    int dateIndex = cursor.getColumnIndexOrThrow("date");
                    
                    do {
                        String sender = cursor.getString(addressIndex);
                        String body = cursor.getString(bodyIndex);
                        long smsDate = cursor.getLong(dateIndex);
                        
                        // Check if sender looks like a bank code (usually has a hyphen like AX-KOTAKB, JD-KOTAKD)
                        boolean isBankSender = sender != null && (sender.contains("-") || sender.contains("KOTAK") || sender.contains("SBI") || sender.contains("HDFC") || sender.contains("ICICI") || sender.contains("AXIS"));
                        
                        if (isBankSender) {
                            // Extract amount robustly
                            Pattern amountPattern = Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([\\d,]+\\.?\\d*)");
                            Matcher amountMatcher = amountPattern.matcher(body);
                            double amount = -1;
                            if (amountMatcher.find()) {
                                amount = Double.parseDouble(amountMatcher.group(1).replace(",", ""));
                            } else {
                                Pattern altAmountPattern = Pattern.compile("(?i)([\\d,]+\\.?\\d*)\\s*(?:rs\\.?|inr|₹)");
                                Matcher altMatcher = altAmountPattern.matcher(body);
                                if (altMatcher.find()) {
                                    amount = Double.parseDouble(altMatcher.group(1).replace(",", ""));
                                }
                            }
                            
                            if (amount > 0) {
                                String lowerBody = body.toLowerCase();
                                boolean isDebit = lowerBody.contains("paid") || lowerBody.contains("sent") || lowerBody.contains("debited") || lowerBody.contains("spent");
                                boolean isCredit = lowerBody.contains("credited") || lowerBody.contains("received") || lowerBody.contains("added");
                                
                                if (isDebit || isCredit) {
                                    totalFound++;
                                    
                                    // Extract reference number
                                    String refNum = null;
                                    Pattern refPattern = Pattern.compile("(?i)(?:ref|upi|txn|id|reference)\\.?\\s*(?:no\\.?|num\\.?|number)?\\s*(\\d{12}|\\d{6,})");
                                    Matcher refMatcher = refPattern.matcher(body);
                                    if (refMatcher.find()) {
                                        refNum = refMatcher.group(1);
                                    }
                                    
                                    String finalRefNum = refNum != null ? refNum : "SMS_" + (smsDate / 1000) + "_" + (int)(amount);
                                    
                                    // Duplicate check
                                    LocalTransaction existing = dao.getTransactionByRefNum(finalRefNum);
                                    if (existing == null) {
                                        // Save transaction
                                        LocalTransaction tx = new LocalTransaction();
                                        tx.id = UUID.randomUUID().toString();
                                        tx.amount = amount;
                                        tx.timestamp = smsDate;
                                        
                                        // Estimate merchant
                                        String merchant = "Unknown Merchant";
                                        if (body.contains("to ")) {
                                            int start = body.indexOf("to ") + 3;
                                            String rawMerchant = body.substring(start).trim();
                                            int onIndex = rawMerchant.indexOf("on ");
                                            if (onIndex > 0) {
                                                merchant = rawMerchant.substring(0, onIndex).trim();
                                            } else {
                                                merchant = rawMerchant;
                                            }
                                        } else if (body.contains("at ")) {
                                            int start = body.indexOf("at ") + 3;
                                            merchant = body.substring(start).trim();
                                        }
                                        
                                        if (merchant.length() > 30) {
                                            merchant = merchant.substring(0, 30).trim();
                                        }
                                        
                                        tx.merchant = merchant;
                                        tx.referenceNumber = finalRefNum;
                                        tx.category = "Other";
                                        tx.transactionType = isDebit ? "DEBIT" : "CREDIT";
                                        tx.bank = sender.contains("KOTAK") ? "Kotak Bank" : sender;
                                        tx.isDeleted = false;
                                        tx.updatedAt = System.currentTimeMillis();
                                        
                                        dao.insertOrReplace(tx);
                                        newSaved++;
                                    }
                                }
                            }
                        }
                    } while (cursor.moveToNext());
                }
                
                final int finalFound = totalFound;
                final int finalSaved = newSaved;
                new Handler(Looper.getMainLooper()).post(() -> {
                    AppLogger.log("SMS Scan: Found " + finalFound + " alerts, imported " + finalSaved + " new transactions.");
                    Toast.makeText(MainActivity.this, "Scan complete! Imported " + finalSaved + " new transactions.", Toast.LENGTH_LONG).show();
                    updateSyncStats();
                    
                    if (finalSaved > 0) {
                        triggerSyncNow();
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                AppLogger.log("SMS Scan Error: " + e.getMessage());
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }
}
