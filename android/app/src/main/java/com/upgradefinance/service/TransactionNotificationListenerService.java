package com.upgradefinance.service;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import com.upgradefinance.db.AppDatabase;
import com.upgradefinance.db.TransactionDao;
import com.upgradefinance.model.LocalTransaction;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransactionNotificationListenerService extends NotificationListenerService {
    private static final String TAG = "NotificationListener";

    // Target UPI applications package IDs
    private static final String GPAY_PKG = "com.google.android.apps.nbu.paisa.user";
    private static final String PHONEPE_PKG = "com.phonepe.app";
    private static final String PAYTM_PKG = "net.one97.paytm";

    // Notification body matcher: e.g. "Paid Rs. 500 to Swiggy"
    private static final Pattern PAYMENT_PATTERN = Pattern.compile("(?i)(?:paid|sent|received|added|credited|debited)\\s*(?:rs\\.?|inr)\\s*([\\d,]+\\.?\\d*)");

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        if (!GPAY_PKG.equals(pkg) && !PHONEPE_PKG.equals(pkg) && !PAYTM_PKG.equals(pkg)) {
            return; // Not a payment app push notification
        }

        Notification notification = sbn.getNotification();
        if (notification == null || notification.extras == null) return;

        CharSequence title = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = notification.extras.getCharSequence(Notification.EXTRA_TEXT);

        if (text == null) return;

        Log.d(TAG, "Payment app notification intercepted. Pkg: " + pkg + " Text: " + text);

        parseAndSaveNotification(text.toString(), pkg);
    }

    private void parseAndSaveNotification(String body, String pkg) {
        Matcher matcher = PAYMENT_PATTERN.matcher(body);
        if (!matcher.find()) return;

        try {
            double amount = Double.parseDouble(matcher.group(1).replace(",", ""));
            boolean isDebit = body.toLowerCase().contains("paid") || body.toLowerCase().contains("sent") || body.toLowerCase().contains("debited");

            String merchant = "Unknown Merchant";
            if (body.contains("to ")) {
                int start = body.indexOf("to ") + 3;
                merchant = body.substring(start).trim();
            } else if (body.contains("at ")) {
                int start = body.indexOf("at ") + 3;
                merchant = body.substring(start).trim();
            }

            // Estimate Category
            String category = "Other";
            if (merchant.toLowerCase().contains("swiggy") || merchant.toLowerCase().contains("zomato")) category = "Food";
            else if (merchant.toLowerCase().contains("amazon") || merchant.toLowerCase().contains("flipkart")) category = "Shopping";
            else if (merchant.toLowerCase().contains("uber") || merchant.toLowerCase().contains("rapido")) category = "Travel";

            final String finalMerchant = merchant;
            final String finalCategory = category;
            final double finalAmount = amount;
            final boolean finalIsDebit = isDebit;
            
            String bankName = "UPI";
            if (pkg.contains("paisa")) bankName = "Google Pay";
            else if (pkg.contains("phonepe")) bankName = "PhonePe";
            else if (pkg.contains("paytm")) bankName = "Paytm";
            final String finalBank = bankName;

            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
                TransactionDao dao = db.transactionDao();

                // Build a LocalTransaction item
                LocalTransaction tx = new LocalTransaction();
                tx.id = UUID.randomUUID().toString();
                tx.amount = finalAmount;
                tx.timestamp = System.currentTimeMillis();
                tx.merchant = finalMerchant;
                tx.category = finalCategory;
                tx.transactionType = finalIsDebit ? "DEBIT" : "CREDIT";
                tx.bank = finalBank;
                tx.isDeleted = false;
                tx.updatedAt = System.currentTimeMillis();
                // Reference number might arrive later in bank SMS. We create a placeholder.
                tx.referenceNumber = "NOTIF_" + (System.currentTimeMillis() / 1000) + "_" + (int)(amount);

                dao.insertOrReplace(tx);
                Log.d(TAG, "Saved push transaction: ₹" + finalAmount + " for " + finalMerchant);
            });

        } catch (Exception e) {
            Log.e(TAG, "Error matching notification regex", e);
        }
    }
}
