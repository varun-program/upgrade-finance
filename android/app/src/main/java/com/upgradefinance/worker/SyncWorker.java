package com.upgradefinance.worker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.upgradefinance.db.AppDatabase;
import com.upgradefinance.db.TransactionDao;
import com.upgradefinance.model.LocalTransaction;
import com.upgradefinance.utils.AppLogger;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncWorker extends Worker {
    private static final String TAG = "SyncWorker";
    private static final String PREFS_NAME = "UpgradeFinancePrefs";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString("auth_token", null);

        if (token == null) {
            Log.d(TAG, "No sync token found. Skipping cloud sync.");
            AppLogger.log("Sync: Skipped (no login token)");
            return Result.success(); // Local-only mode, skip sync
        }

        long lastSync = prefs.getLong("last_sync_timestamp", 0);
        AppDatabase db = AppDatabase.getDatabase(context);
        TransactionDao dao = db.transactionDao();

        try {
            // Find local modifications since last sync
            List<LocalTransaction> changes = dao.getChangesSince(lastSync);
            AppLogger.log("Sync: Initiated. Found " + changes.size() + " unsynced local changes.");
            if (changes.isEmpty()) {
                Log.d(TAG, "No local changes to sync.");
            }

            // Sync request payload
            Gson gson = new Gson();
            Map<String, Object> payload = new HashMap<>();
            payload.put("lastSyncTimestamp", lastSync);
            payload.put("transactions", changes);

            URL url = new URL("https://upgrade-finance.onrender.com/api/sync"); // Connects to production Render backend
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setDoOutput(true);

            // Write payload
            try (OutputStream os = conn.getOutputStream()) {
                os.write(gson.toJson(payload).getBytes());
                os.flush();
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                // Parse response
                InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                Map<String, Object> response = gson.fromJson(reader, new TypeToken<Map<String, Object>>(){}.getType());
                
                long newSyncTime = ((Double) response.get("syncTimestamp")).longValue();
                
                // Read and save server changes
                String transactionsJson = gson.toJson(response.get("transactions"));
                List<LocalTransaction> serverTxs = gson.fromJson(transactionsJson, new TypeToken<List<LocalTransaction>>(){}.getType());
                
                if (serverTxs != null && !serverTxs.isEmpty()) {
                    dao.insertOrReplaceAll(serverTxs);
                }

                // Save new sync timestamp
                prefs.edit().putLong("last_sync_timestamp", newSyncTime).apply();
                Log.d(TAG, "Sync successfully completed at " + newSyncTime);
                AppLogger.log("Sync: Success! Downloaded cloud updates.");
                return Result.success();
            } else {
                Log.e(TAG, "Server returned sync error code: " + code);
                AppLogger.log("Sync: Failed (HTTP " + code + ")");
                return Result.retry();
            }

        } catch (Exception e) {
            Log.e(TAG, "Sync execution failed", e);
            AppLogger.log("Sync Error: " + e.getMessage());
            return Result.retry();
        }
    }
}
